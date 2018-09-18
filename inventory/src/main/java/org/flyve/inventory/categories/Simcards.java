/**
 * LICENSE
 * <p>
 * This file is part of Flyve MDM Inventory Library for Android.
 * <p>
 * Inventory Library for Android is a subproject of Flyve MDM.
 * Flyve MDM is a mobile device management software.
 * <p>
 * Flyve MDM is free software: you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 3
 * of the License, or (at your option) any later version.
 * <p>
 * Flyve MDM is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 * ---------------------------------------------------------------------
 *
 * @author Rafael Hernandez - <rhernandez@teclib.com>
 * @copyright Copyright Teclib. All rights reserved.
 * @copyright Copyright FusionInventory.
 * @license GPLv3 https://www.gnu.org/licenses/gpl-3.0.html
 * @link https://github.com/flyve-mdm/android-inventory-library
 * @link http://flyve.org/android-inventory-library/
 * @link https://flyve-mdm.com
 * ---------------------------------------------------------------------
 */

package org.flyve.inventory.categories;

import android.content.Context;
import android.telephony.SubscriptionInfo;
import android.telephony.SubscriptionManager;
import android.telephony.TelephonyManager;

import org.flyve.inventory.FILog;

import java.lang.reflect.Method;
import java.util.List;

/**
 * This class get all the information of the Simcards Devices
 */
public class Simcards extends Categories {

    /*
     * The serialization runtime associates with each serializable class a version number,
     * called a serialVersionUID, which is used during deserialization to verify that the sender
     * and receiver of a serialized object have loaded classes for that object that are compatible
     * with respect to serialization. If the receiver has loaded a class for the object that has a
     * different serialVersionUID than that of the corresponding sender's class, then deserialization
     * will result in an  InvalidClassException
     *
     *  from: https://stackoverflow.com/questions/285793/what-is-a-serialversionuid-and-why-should-i-use-it
     */
    private static final long serialVersionUID = -5532129156981574844L;

    private TelephonyManager mTM;

    /**
     * Indicates whether some other object is "equal to" this one
     * @param obj the reference object with which to compare
     * @return boolean true if the object is the same as the one given in argument
     */
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        return (!super.equals(obj));
    }

    /**
     * Returns a hash code value for the object
     * @return int a hash code value for the object
     */
    @Override
    public int hashCode() {
        int hash = super.hashCode();
        hash = 89 * hash + (this.mTM != null ? this.mTM.hashCode() : 0);
        return hash;
    }

    /**
     * This constructor load the context and the Simcards information
     * @param xCtx Context where this class work
     */
    public Simcards(Context xCtx) {
        super(xCtx);

        try {
            /*
             * Starting SimCards information retrieval
             */
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
                List<SubscriptionInfo> multipleSim = getMultipleSim(xCtx);
                if (multipleSim != null && multipleSim.size() > 0) {
                    for (SubscriptionInfo info : multipleSim) {

                        Category c = new Category("SIMCARDS", "simcards");

                        c.put("COUNTRY", new CategoryValue(info.getCountryIso(), "COUNTRY", "country"));
                        String operatorCode = info.getMcc() + "" + info.getMnc();
                        c.put("OPERATOR_CODE", new CategoryValue(operatorCode, "OPERATOR_CODE", "operatorCode"));
                        c.put("OPERATOR_NAME", new CategoryValue(info.getCarrierName().toString(), "OPERATOR_NAME", "operatorName"));
                        c.put("SERIAL", new CategoryValue(info.getIccId(), "SERIAL", "serial"));
                        c.put("STATE", new CategoryValue(getState(xCtx, info.getSimSlotIndex()), "STATE", "state"));
                        c.put("LINE_NUMBER", new CategoryValue(info.getNumber(), "LINE_NUMBER", "lineNumber"));
                        String subscriberId = String.valueOf(info.getSubscriptionId());
                        c.put("SUBSCRIBER_ID", new CategoryValue(subscriberId, "SUBSCRIBER_ID", "subscriberId"));

                        this.add(c);
                    }
                }
            } else {
                mTM = (TelephonyManager) xCtx.getSystemService(Context.TELEPHONY_SERVICE);
                assert mTM != null;
                if (getState(xCtx, 0).equals("SIM_STATE_READY")) {
                    Category c = new Category("SIMCARDS", "simcards");

                    c.put("COUNTRY", new CategoryValue(getCountry(), "COUNTRY", "country"));
                    c.put("OPERATOR_CODE", new CategoryValue(getOperatorCode(), "OPERATOR_CODE", "operatorCode"));
                    c.put("OPERATOR_NAME", new CategoryValue(getOperatorName(), "OPERATOR_NAME", "operatorName"));
                    c.put("SERIAL", new CategoryValue(getSerial(), "SERIAL", "serial"));
                    c.put("STATE", new CategoryValue(getState(xCtx, 0), "STATE", "state"));
                    c.put("LINE_NUMBER", new CategoryValue(getLineNumber(), "LINE_NUMBER", "lineNumber"));
                    c.put("SUBSCRIBER_ID", new CategoryValue(getSubscriberId(), "SUBSCRIBER_ID", "subscriberId"));

                    this.add(c);
                }
            }
        } catch (Exception ex) {
            FILog.e(ex.getMessage());
        }
    }


    /**
     * Get list sim card
     * @return true API >= 22
     */
    public List<SubscriptionInfo> getMultipleSim(Context xCtx) {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP_MR1) {
            SubscriptionManager subscriptionManager = (SubscriptionManager) xCtx.getSystemService(Context.TELEPHONY_SUBSCRIPTION_SERVICE);
            return subscriptionManager.getActiveSubscriptionInfoList();
        } else {
            return null;
        }
    }

    /**
     * Get the state Sim by slot
     * @return string the Simcard state
     */
    private int getSIMStateBySlot(Context context, int slotID) {
        TelephonyManager telephony = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
        try {
            Class<?> telephonyClass = Class.forName(telephony.getClass().getName());
            Class<?>[] parameter = new Class[1];
            parameter[0] = int.class;
            Method getSimState = telephonyClass.getMethod("getSimState", parameter);
            Object[] obParameter = new Object[1];
            obParameter[0] = slotID;
            Object obPhone = getSimState.invoke(telephony, obParameter);
            if (obPhone != null) {
                return Integer.parseInt(obPhone.toString());
            }
        } catch (Exception e) {
            FILog.e(e.getMessage());
        }

        return 0;
    }

    /**
     * Get the country
     * @return string the ISO country code
     */
    public String getCountry() {
        return mTM.getSimCountryIso();
    }

    /**
     * Get the operator code
     * @return the Mobile Country Code and Mobile Network Code of the provider of the sim
     */
    public String getOperatorCode() {
        return mTM.getSimOperator();
    }

    /**
     * Get the operator name
     * @return string the Service Provider Name
     */
    public String getOperatorName() {
        return mTM.getSimOperatorName();
    }

    /**
     * Get the serial number of the Sim
     * @return string the serial number
     */
    public String getSerial() {
        return mTM.getSimSerialNumber();
    }

    /**
     * Get state of the Simcard
     * @return string the Simcard state
     * @param xCtx
     */
    public String getState(Context xCtx, int slotId) {
        int simState = mTM != null ? mTM.getSimState() : getSIMStateBySlot(xCtx, slotId);
        String mState;
        switch (simState) {
            case TelephonyManager.SIM_STATE_ABSENT:
                mState = "SIM_STATE_ABSENT";
                break;
            case TelephonyManager.SIM_STATE_NETWORK_LOCKED:
                mState = "SIM_STATE_NETWORK_LOCKED";
                break;
            case TelephonyManager.SIM_STATE_PIN_REQUIRED:
                mState = "SIM_STATE_PIN_REQUIRED";
                break;
            case TelephonyManager.SIM_STATE_PUK_REQUIRED:
                mState = "SIM_STATE_PUK_REQUIRED";
                break;
            case TelephonyManager.SIM_STATE_READY:
                mState = "SIM_STATE_READY";
                break;
            case TelephonyManager.SIM_STATE_UNKNOWN:
                mState = "SIM_STATE_UNKNOWN";
                break;
            default:
                mState = "SIM_STATE_UNKNOWN";
                break;
        }
        return mState;
    }

    /**
     * Get the line number
     * @return string the phone number for line 1
     */
    public String getLineNumber() {
        return mTM.getLine1Number();
    }

    /**
     * Get the subscriber ID
     * @return string the unique subscriber ID
     */
    public String getSubscriberId() {
        return mTM.getSubscriberId();
    }
}
