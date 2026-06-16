package com.dicteditor.percynguyen92.aitranslateportal;

import com.dicteditor.percynguyen92.aitranslateportal.AiSuggestionParcel;

interface IAiPortalCallback {
    void onSuccess(String requestId, in AiSuggestionParcel result);
    void onError(String requestId, int errorCode, String message);
}
