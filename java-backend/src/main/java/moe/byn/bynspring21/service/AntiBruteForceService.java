package moe.byn.bynspring21.service;

public interface AntiBruteForceService {
    void checkBeforeLogin(String userId);

    void checkBeforeRegister();

    void checkBeforeEmailCodeVerification(String userId);

    void checkBeforeSendEmailCode(String userEmail);
}
