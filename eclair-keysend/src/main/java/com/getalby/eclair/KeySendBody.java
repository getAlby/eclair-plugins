package com.getalby.eclair;

import java.util.HashMap;
import java.util.Map;

public class KeySendBody {
    private final String nodeId;
    private final int amountMsat;
    private final Map<String, String> customTlvs;

    public String getNodeId() {
        return nodeId;
    }

    public int getAmountMsat() {
        return amountMsat;
    }

    public Map<String, String> getCustomTlvs() {
        return customTlvs;
    }

    public KeySendBody(String nodeId, int amountMsat, Map<String, String> customTlvs) {
        this.nodeId = nodeId;
        this.amountMsat = amountMsat;
        this.customTlvs = customTlvs;
    }

    public KeySendBody() {
        this.nodeId = "";
        this.amountMsat = 0;
        this.customTlvs = new HashMap<>();
    }
}
