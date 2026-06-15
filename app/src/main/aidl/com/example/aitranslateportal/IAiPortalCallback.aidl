package com.example.aitranslateportal;

import com.example.aitranslateportal.AiSuggestionParcel;

interface IAiPortalCallback {
    void onSuccess(String requestId, in AiSuggestionParcel result);
    void onError(String requestId, int errorCode, String message);
}
