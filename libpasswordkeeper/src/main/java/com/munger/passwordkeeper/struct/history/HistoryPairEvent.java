package com.munger.passwordkeeper.struct.history;

/**
 * Created by codymunger on 11/19/16.
 */
public abstract class HistoryPairEvent extends HistoryEvent {
    public String pairid = "";

    @Override
    public void fromString(String input) {
        int spcIdx = input.indexOf(' ');
        String part = (spcIdx > -1) ? input.substring(0, spcIdx) : input.substring(0);

        pairid = part;

        input = input.substring(spcIdx + 1);
        super.fromString(input);
    }

    @Override
    public String toString() {
        String ret = pairid + " ";
        ret += super.toString();

        return ret;
    }

    @Override
    public String getPairIDSignature() {
        return "pair:" + id + "-" + pairid;
    }

    @Override
    public String getPropertySignature() {
        return type.ordinal() + " " + id + " " + pairid + " " + property;
    }
}
