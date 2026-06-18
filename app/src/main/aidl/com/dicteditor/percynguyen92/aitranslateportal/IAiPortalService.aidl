package com.dicteditor.percynguyen92.aitranslateportal;

import com.dicteditor.percynguyen92.aitranslateportal.IAiPortalCallback;

interface IAiPortalService {
    boolean ping();
    void getSuggestion(String requestId, String chinese, IAiPortalCallback callback);
    void cancelRequest(String requestId);
    String getStatus();
    void clearCache(String chinese);
}
