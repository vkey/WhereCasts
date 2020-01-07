package com.krisdb.wearcasts.Models;

public class WatchStatus {
    private boolean premiumconfirm, thirdparty;

    public boolean getPremiumConfirm() {
        return premiumconfirm;
    }

    public void setPremiumConfirm(final boolean confirm) {
        this.premiumconfirm = confirm;
    }

    public boolean getThirdParty() {
        return thirdparty;
    }

    public void setThirdParty(final boolean thirdparty) {
        this.thirdparty = thirdparty;
    }

}
