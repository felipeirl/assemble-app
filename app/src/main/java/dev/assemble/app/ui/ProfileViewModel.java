package dev.assemble.app.ui;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import dev.assemble.app.data.local.AuthException;
import dev.assemble.app.data.local.SessionStore;
import dev.assemble.app.data.model.UserProfile;
import dev.assemble.app.data.repository.Callback;
import dev.assemble.app.data.repository.ProfileRepository;

/**
 * Profile (tela do Vetor; aqui só estado + dados).
 * Garante Auth anônimo e expõe o UID ativo como identidade.
 */
public class ProfileViewModel extends ViewModel {
    private final SessionStore session;
    private final ProfileRepository profiles;

    private final MutableLiveData<String> uid = new MutableLiveData<>();
    private final MutableLiveData<UserProfile> profile = new MutableLiveData<>();
    private final MutableLiveData<String> error = new MutableLiveData<>();
    private final MutableLiveData<Boolean> busy = new MutableLiveData<>(false);

    public ProfileViewModel(SessionStore session, ProfileRepository profiles) {
        this.session = session;
        this.profiles = profiles;
    }

    public LiveData<String> getUid() {
        return uid;
    }

    public LiveData<UserProfile> getProfile() {
        return profile;
    }

    public LiveData<String> getError() {
        return error;
    }

    public LiveData<Boolean> getBusy() {
        return busy;
    }

    public void ensureSession() {
        busy.postValue(true);
        session.ensureAnonymous().addOnCompleteListener(task -> {
            busy.postValue(false);
            if (!task.isSuccessful() || task.getResult() == null) {
                Exception e = task.getException();
                error.postValue(e != null ? e.getMessage() : "auth/sign-in-failed");
                return;
            }
            String id = task.getResult().getUid();
            uid.postValue(id);
            load(id);
        });
    }

    public void load(String uidValue) {
        busy.postValue(true);
        profiles.load(uidValue, new Callback<UserProfile>() {
            @Override
            public void onSuccess(UserProfile value) {
                busy.postValue(false);
                profile.postValue(value);
            }

            @Override
            public void onError(Exception e) {
                busy.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }

    public void save(UserProfile value) {
        String id = uid.getValue();
        if (id == null) {
            try {
                id = session.requireUid();
            } catch (AuthException e) {
                error.postValue(e.getMessage());
                return;
            }
        }
        busy.postValue(true);
        profiles.save(id, value, new Callback<Void>() {
            @Override
            public void onSuccess(Void ignored) {
                busy.postValue(false);
                profile.postValue(value);
            }

            @Override
            public void onError(Exception e) {
                busy.postValue(false);
                error.postValue(e.getMessage());
            }
        });
    }
}
