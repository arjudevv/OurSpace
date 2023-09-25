package com.example.ourspace.activities;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.example.ourspace.Utilities.Constants;
import com.example.ourspace.Utilities.PreferenceManager;
import com.example.ourspace.databinding.ActivitySignInBinding;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Objects;

import javax.crypto.Cipher;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

public class SignInActivity extends AppCompatActivity {

    private ActivitySignInBinding binding;
    private PreferenceManager preferenceManager;
    private FirebaseAuth firebaseAuth;
    KeyGenerator keyGenerator;
    SecretKey secretKey;
    byte[] IV = new byte[16];
    SecureRandom random;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        firebaseAuth = FirebaseAuth.getInstance();
        FirebaseUser user = firebaseAuth.getCurrentUser();
        preferenceManager = new PreferenceManager(getApplicationContext());
        if (preferenceManager.getBoolean(Constants.KEY_IS_SIGNED_IN) && user != null) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            startActivity(intent);
            finish();
        }
        binding = ActivitySignInBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setListeners();
        encryptData();
        getFirebaseData();
    }

    private void getFirebaseData() {
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        /*ITH SCNAAN MWONU*/
        DocumentReference documentReference = db.collection("users").document("w7Xc1BronujCnVEy9Yrm");
        documentReference.get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        DocumentSnapshot document = task.getResult();
                        if (document.exists()) {
                            String encryptedPass = document.getString("password");
                            if (encryptedPass != null) {
                                Log.e("Encrypted Pass", encryptedPass);
                            }
                        }
                    }
                });

    }

    private void encryptData() {
        try {
            keyGenerator = KeyGenerator.getInstance("AES");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        keyGenerator.init(256);
        secretKey = keyGenerator.generateKey();

        random = new SecureRandom();
        random.nextBytes(IV);
    }



    public static String decrypt(byte[] cipherText, SecretKey key, byte[] IV)
    {
        try {
            Cipher cipher = Cipher.getInstance("AES");
            SecretKeySpec keySpec = new SecretKeySpec(key.getEncoded(), "AES");
            IvParameterSpec ivSpec = new IvParameterSpec(IV);
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec);
            byte[] decryptedText = cipher.doFinal(cipherText);
            return new String(decryptedText);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private void setListeners() {
        binding.textCreateNewAc.setOnClickListener(v ->
                startActivity(new Intent(getApplicationContext(), SignUpActivity.class)));
        binding.buttonSignIn.setOnClickListener(v -> {
            if (isValidSignInDetails()) {
                signIn();
            }
        });
    }

    private void signIn() {

        loading(true);
        FirebaseFirestore database = FirebaseFirestore.getInstance();
        database.collection(Constants.KEY_COLLECTION_USERS)
                .whereEqualTo(Constants.KEY_EMAIL, binding.inputEmail.getText().toString())
                .whereEqualTo(Constants.KEY_PASSWORD, binding.inputPassword.getText().toString())
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful() && task.getResult() != null
                            && task.getResult().getDocuments().size() > 0) {
                        DocumentSnapshot documentSnapshot = task.getResult().getDocuments().get(0);
                        preferenceManager.putBoolean(Constants.KEY_IS_SIGNED_IN, true);
                        preferenceManager.putString(Constants.KEY_USER_ID, documentSnapshot.getId());
                        preferenceManager.putString(Constants.KEY_NAME, documentSnapshot.getString(Constants.KEY_NAME));
                        preferenceManager.putString(Constants.KEY_IMAGE, documentSnapshot.getString(Constants.KEY_IMAGE));
                        String IV = preferenceManager.getString("IV");
                        signInWithEmail();
//                        Intent intent = new Intent(getApplicationContext(), MainActivity.class);
//                        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
//                        startActivity(intent);
                    } else {
                        loading(false);
                        showToast("Unable to sign in");
                    }
                });
    }

    private void loading(Boolean isLoading) {
        if (isLoading) {
            binding.buttonSignIn.setVisibility(View.INVISIBLE);
            binding.progressBar.setVisibility(View.VISIBLE);
        } else {
            binding.progressBar.setVisibility(View.INVISIBLE);
            binding.buttonSignIn.setVisibility(View.VISIBLE);
        }
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    private Boolean isValidSignInDetails() {
        if (binding.inputEmail.getText().toString().trim().isEmpty()) {
            showToast("Enter Email");
            return false;
        } else if (!Patterns.EMAIL_ADDRESS.matcher(binding.inputEmail.getText().toString()).matches()) {
            showToast("Enter valid Email");
            return false;
        } else if (binding.inputPassword.getText().toString().trim().isEmpty()) {
            showToast("Enter password");
            return false;
        } else {
            return true;
        }
    }

    private void signInWithEmail() {
        firebaseAuth.signInWithEmailAndPassword(binding.inputEmail.getText().toString(), binding.inputPassword.getText().toString()).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                checkEmailVerification();
            } else {
                showToast("Login failed!");
                loading(false);
            }
        });
    }

    private void checkEmailVerification() {
        FirebaseUser firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        boolean emailFlag = Objects.requireNonNull(firebaseUser).isEmailVerified();
        if (emailFlag) {
            Intent intent = new Intent(getApplicationContext(), MainActivity.class);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        } else {
            showToast("Verify your email");
            loading(false);
            firebaseAuth.signOut();
        }
    }
}