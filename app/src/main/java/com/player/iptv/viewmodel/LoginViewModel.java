package com.player.iptv.viewmodel;

import android.app.Application;

import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.player.iptv.data.CredentialRepository;

import io.reactivex.android.schedulers.AndroidSchedulers;
import io.reactivex.disposables.CompositeDisposable;

public class LoginViewModel extends AndroidViewModel {

    private final CredentialRepository repository;
    private final MutableLiveData<Boolean> loginResult = new MutableLiveData<>();
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> isLoading = new MutableLiveData<>(false);
    private final CompositeDisposable disposables = new CompositeDisposable();

    public LoginViewModel(Application application) {
        super(application);
        repository = new CredentialRepository(application);
    }

    public LiveData<Boolean> getLoginResult() { return loginResult; }
    public LiveData<String> getErrorMessage() { return errorMessage; }
    public LiveData<Boolean> getIsLoading() { return isLoading; }

    public boolean isLoggedIn() {
        return repository.isLoggedIn();
    }

    public void login(String serverUrl, String username, String password) {
        if (serverUrl.isEmpty() || username.isEmpty() || password.isEmpty()) {
            errorMessage.setValue("Preencha todos os campos");
            return;
        }

        isLoading.setValue(true);
        disposables.add(repository.login(serverUrl, username, password)
            .observeOn(AndroidSchedulers.mainThread())
            .subscribe(success -> {
                isLoading.setValue(false);
                loginResult.setValue(success);
                if (!success) {
                    errorMessage.setValue("Falha na autenticação");
                }
            }, throwable -> {
                isLoading.setValue(false);
                errorMessage.setValue("Erro: " + throwable.getMessage());
            }));
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        disposables.clear();
    }
}
