// JNI bridge over the official Discord Social SDK (discordpp::Client) for Rich Presence via
// real OAuth2 (PKCE, public client) account-linking — replaces the legacy kizzy token-scraping
// and gateway-impersonation path. See the project plan for the full architecture.
#define DISCORDPP_IMPLEMENTATION
#include <discordpp.h>

#include <jni.h>
#include <memory>
#include <string>

namespace {

constexpr auto kBridgeClass = "com/arturo254/opentune/utils/DiscordSocialSdkBridge";

JavaVM* g_jvm = nullptr;
std::unique_ptr<discordpp::Client> g_client;
// Kept alive between nativeCreateAuthorizationVerifier -> nativeAuthorize -> nativeGetToken so
// the SDK-generated PKCE challenge/verifier pair (including its S256 method) round-trips intact,
// instead of being reconstructed from raw strings passed back and forth over JNI.
std::unique_ptr<discordpp::AuthorizationCodeVerifier> g_verifier;

JNIEnv* AttachCurrentThread(bool* didAttach) {
    JNIEnv* env = nullptr;
    if (g_jvm->GetEnv(reinterpret_cast<void**>(&env), JNI_VERSION_1_6) == JNI_EDETACHED) {
        g_jvm->AttachCurrentThread(&env, nullptr);
        *didAttach = true;
    } else {
        *didAttach = false;
    }
    return env;
}

void DetachIfNeeded(bool didAttach) {
    if (didAttach) g_jvm->DetachCurrentThread();
}

jclass BridgeClass(JNIEnv* env) {
    return env->FindClass(kBridgeClass);
}

jstring ToJString(JNIEnv* env, const std::string& s) {
    return env->NewStringUTF(s.c_str());
}

std::string ToStdString(JNIEnv* env, jstring s) {
    if (s == nullptr) return {};
    const char* chars = env->GetStringUTFChars(s, nullptr);
    std::string result(chars);
    env->ReleaseStringUTFChars(s, chars);
    return result;
}

// Shared by nativeGetToken and nativeRefreshToken — both use discordpp::Client::TokenExchangeCallback.
discordpp::Client::TokenExchangeCallback MakeTokenExchangeCallback() {
    return [](discordpp::ClientResult result, std::string accessToken, std::string refreshToken,
              discordpp::AuthorizationTokenType, int32_t expiresIn, std::string scopes) {
        bool didAttach = false;
        JNIEnv* cbEnv = AttachCurrentThread(&didAttach);
        jclass clazz = BridgeClass(cbEnv);
        jmethodID method = cbEnv->GetStaticMethodID(
            clazz, "onTokenResult",
            "(ZLjava/lang/String;Ljava/lang/String;ILjava/lang/String;Ljava/lang/String;)V");
        cbEnv->CallStaticVoidMethod(
            clazz, method, static_cast<jboolean>(result.Successful()),
            ToJString(cbEnv, accessToken), ToJString(cbEnv, refreshToken),
            static_cast<jint>(expiresIn), ToJString(cbEnv, scopes),
            ToJString(cbEnv, result.Error()));
        cbEnv->DeleteLocalRef(clazz);
        DetachIfNeeded(didAttach);
    };
}

}  // namespace

extern "C" JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void*) {
    g_jvm = vm;
    return JNI_VERSION_1_6;
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeCreateClient(JNIEnv*, jobject) {
    g_client = std::make_unique<discordpp::Client>();
    if (!*g_client) return JNI_FALSE;

    g_client->SetStatusChangedCallback(
        [](discordpp::Client::Status status, discordpp::Client::Error error, int32_t errorDetail) {
            bool didAttach = false;
            JNIEnv* env = AttachCurrentThread(&didAttach);
            jclass clazz = BridgeClass(env);
            jmethodID method = env->GetStaticMethodID(clazz, "onStatusChanged", "(III)V");
            env->CallStaticVoidMethod(
                clazz, method,
                static_cast<jint>(status), static_cast<jint>(error), static_cast<jint>(errorDetail));
            env->DeleteLocalRef(clazz);
            DetachIfNeeded(didAttach);
        });

    return JNI_TRUE;
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeRunCallbacks(JNIEnv*, jobject) {
    discordpp::RunCallbacks();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeGetDefaultPresenceScopes(
    JNIEnv* env, jobject) {
    return ToJString(env, discordpp::Client::GetDefaultPresenceScopes());
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeAuthorize(
    JNIEnv* env, jobject, jlong clientId, jstring scopes) {
    if (!g_client) return;
    g_verifier = std::make_unique<discordpp::AuthorizationCodeVerifier>(
        g_client->CreateAuthorizationCodeVerifier());

    discordpp::AuthorizationArgs args{};
    args.SetClientId(static_cast<uint64_t>(clientId));
    args.SetScopes(ToStdString(env, scopes));
    args.SetCodeChallenge(g_verifier->Challenge());

    g_client->Authorize(
        args,
        [](discordpp::ClientResult result, std::string code, std::string redirectUri) {
            bool didAttach = false;
            JNIEnv* cbEnv = AttachCurrentThread(&didAttach);
            jclass clazz = BridgeClass(cbEnv);
            jmethodID method = cbEnv->GetStaticMethodID(
                clazz, "onAuthorizeResult",
                "(ZLjava/lang/String;Ljava/lang/String;Ljava/lang/String;)V");
            cbEnv->CallStaticVoidMethod(
                clazz, method, static_cast<jboolean>(result.Successful()),
                ToJString(cbEnv, code), ToJString(cbEnv, redirectUri),
                ToJString(cbEnv, result.Error()));
            cbEnv->DeleteLocalRef(clazz);
            DetachIfNeeded(didAttach);
        });
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeGetToken(
    JNIEnv* env, jobject, jlong applicationId, jstring code, jstring redirectUri) {
    if (!g_client || !g_verifier) return;
    g_client->GetToken(
        static_cast<uint64_t>(applicationId), ToStdString(env, code), g_verifier->Verifier(),
        ToStdString(env, redirectUri), MakeTokenExchangeCallback());
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeRefreshToken(
    JNIEnv* env, jobject, jlong applicationId, jstring refreshToken) {
    if (!g_client) return;
    g_client->RefreshToken(
        static_cast<uint64_t>(applicationId), ToStdString(env, refreshToken),
        MakeTokenExchangeCallback());
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeUpdateToken(
    JNIEnv* env, jobject, jstring accessToken) {
    if (!g_client) return;
    g_client->UpdateToken(
        discordpp::AuthorizationTokenType::Bearer, ToStdString(env, accessToken),
        [](discordpp::ClientResult result) {
            bool didAttach = false;
            JNIEnv* cbEnv = AttachCurrentThread(&didAttach);
            jclass clazz = BridgeClass(cbEnv);
            jmethodID method = cbEnv->GetStaticMethodID(
                clazz, "onUpdateTokenResult", "(ZLjava/lang/String;)V");
            cbEnv->CallStaticVoidMethod(
                clazz, method, static_cast<jboolean>(result.Successful()),
                ToJString(cbEnv, result.Error()));
            cbEnv->DeleteLocalRef(clazz);
            DetachIfNeeded(didAttach);
        });
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeConnect(JNIEnv*, jobject) {
    if (!g_client) return;
    g_client->Connect();
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeDisconnect(JNIEnv*, jobject) {
    if (!g_client) return;
    g_client->Disconnect();
}

extern "C" JNIEXPORT jboolean JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeIsAuthenticated(JNIEnv*, jobject) {
    if (!g_client) return JNI_FALSE;
    return static_cast<jboolean>(g_client->IsAuthenticated());
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeUpdateRichPresence(
    JNIEnv* env, jobject,
    jint activityType,
    jstring details, jstring detailsUrl,
    jstring state,
    jstring largeImageUrl, jstring largeImageText,
    jstring smallImageUrl, jstring smallImageText,
    jlong startTimestampMs, jlong endTimestampMs,
    jstring button1Label, jstring button1Url,
    jstring button2Label, jstring button2Url) {
    if (!g_client) return;
    discordpp::Activity activity{};
    activity.SetType(static_cast<discordpp::ActivityTypes>(activityType));
    activity.SetDetails(ToStdString(env, details));
    activity.SetState(ToStdString(env, state));
    if (detailsUrl != nullptr) {
        activity.SetDetailsUrl(ToStdString(env, detailsUrl));
    }

    if (largeImageUrl != nullptr || smallImageUrl != nullptr) {
        discordpp::ActivityAssets assets{};
        if (largeImageUrl != nullptr) {
            assets.SetLargeImage(ToStdString(env, largeImageUrl));
            if (largeImageText != nullptr) {
                assets.SetLargeText(ToStdString(env, largeImageText));
            }
        }
        if (smallImageUrl != nullptr) {
            assets.SetSmallImage(ToStdString(env, smallImageUrl));
            if (smallImageText != nullptr) {
                assets.SetSmallText(ToStdString(env, smallImageText));
            }
        }
        activity.SetAssets(assets);
    }

    if (startTimestampMs > 0 || endTimestampMs > 0) {
        discordpp::ActivityTimestamps timestamps{};
        if (startTimestampMs > 0) timestamps.SetStart(static_cast<uint64_t>(startTimestampMs));
        if (endTimestampMs > 0) timestamps.SetEnd(static_cast<uint64_t>(endTimestampMs));
        activity.SetTimestamps(timestamps);
    }

    if (button1Label != nullptr && button1Url != nullptr) {
        discordpp::ActivityButton button{};
        button.SetLabel(ToStdString(env, button1Label));
        button.SetUrl(ToStdString(env, button1Url));
        activity.AddButton(button);
    }
    if (button2Label != nullptr && button2Url != nullptr) {
        discordpp::ActivityButton button{};
        button.SetLabel(ToStdString(env, button2Label));
        button.SetUrl(ToStdString(env, button2Url));
        activity.AddButton(button);
    }

    g_client->UpdateRichPresence(activity, [](discordpp::ClientResult result) {
        bool didAttach = false;
        JNIEnv* cbEnv = AttachCurrentThread(&didAttach);
        jclass clazz = BridgeClass(cbEnv);
        jmethodID method = cbEnv->GetStaticMethodID(
            clazz, "onUpdateRichPresenceResult", "(ZLjava/lang/String;)V");
        cbEnv->CallStaticVoidMethod(
            clazz, method, static_cast<jboolean>(result.Successful()),
            ToJString(cbEnv, result.Error()));
        cbEnv->DeleteLocalRef(clazz);
        DetachIfNeeded(didAttach);
    });
}

extern "C" JNIEXPORT void JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeClearRichPresence(
    JNIEnv*, jobject) {
    if (!g_client) return;
    g_client->ClearRichPresence();
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeGetCurrentUserDisplayName(
    JNIEnv* env, jobject) {
    if (!g_client) return nullptr;
    auto user = g_client->GetCurrentUserV2();
    if (!user.has_value()) return nullptr;
    return ToJString(env, user->DisplayName());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeGetCurrentUserUsername(
    JNIEnv* env, jobject) {
    if (!g_client) return nullptr;
    auto user = g_client->GetCurrentUserV2();
    if (!user.has_value()) return nullptr;
    return ToJString(env, user->Username());
}

extern "C" JNIEXPORT jstring JNICALL
Java_com_arturo254_opentune_utils_DiscordSocialSdkBridge_nativeGetCurrentUserAvatarUrl(
    JNIEnv* env, jobject) {
    if (!g_client) return nullptr;
    auto user = g_client->GetCurrentUserV2();
    if (!user.has_value()) return nullptr;
    return ToJString(
        env,
        user->AvatarUrl(
            discordpp::UserHandle::AvatarType::Png, discordpp::UserHandle::AvatarType::Png));
}
