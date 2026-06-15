package com.example.aitranslateportal;

import com.example.aitranslateportal.IAiPortalCallback;

interface IAiPortalService {
    boolean ping();
    void getSuggestion(String requestId, String chinese, IAiPortalCallback callback);
    void cancelRequest(String requestId);
    String getStatus();
}
