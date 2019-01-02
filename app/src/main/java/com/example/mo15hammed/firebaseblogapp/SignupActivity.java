package com.example.mo15hammed.firebaseblogapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthResult;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class SignupActivity extends AppCompatActivity {

    private static final String TAG = "SignupActivity";

    private FirebaseAuth mAuth;

    private ProgressBar mProgressBar;
    private EditText mEmail, mPassword, mConfPassword;
    private Button mLogin, mSignup;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_signup);

        mAuth = FirebaseAuth.getInstance();

        mProgressBar = findViewById(R.id.signup_progress);
        mEmail = findViewById(R.id.edt_email);
        mPassword = findViewById(R.id.edt_password);
        mConfPassword = findViewById(R.id.edt_conf_password);
        mLogin = findViewById(R.id.btn_login);
        mSignup = findViewById(R.id.btn_signup);

        mLogin.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent signupIntent = new Intent(SignupActivity.this, LoginActivity.class);
                startActivity(signupIntent);
//                finish();
            }
        });

        mSignup.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String email = mEmail.getText().toString();
                String password = mPassword.getText().toString();
                String confirmPass = mConfPassword.getText().toString();

                if (!TextUtils.isEmpty(email) && !TextUtils.isEmpty(password) && !TextUtils.isEmpty(confirmPass)) {

                    if (password.equals(confirmPass)) {

                        mProgressBar.setVisibility(View.VISIBLE);
                        signupNewUser(email, password);

                    } else {
                        Toast.makeText(SignupActivity.this, "Confirm Password Correctly !", Toast.LENGTH_SHORT).show();
                    }

                } else {
                    Toast.makeText(SignupActivity.this, "Fill Missing Fields !", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }


    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            sendToMainActivity();
        }

    }

    /**
     * Creates new account for user
     * @param email user email
     * @param password user password
     */
    private void signupNewUser(String email, String password) {

        if (mAuth != null) {

            mAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener(new OnCompleteListener<AuthResult>() {
                @Override
                public void onComplete(@NonNull Task<AuthResult> task) {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    if (task.isSuccessful()) {

                        Intent setupIntent = new Intent(SignupActivity.this , SetupActivity.class);
                        setupIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(setupIntent);
                        finish();

                    } else {
                        // If sign up fails, display a message to the user.
                        Log.w(TAG, "createUserWithEmail:failure : " + task.getException().getMessage());
                        Toast.makeText(SignupActivity.this, "Authentication failed.",
                                Toast.LENGTH_SHORT).show();

                    }
                }
            });

        }

    }


    /**
     * sends user to the MainActivity and finishes the current activity
     */
    private void sendToMainActivity() {
        Intent mainIntent = new Intent(this, MainActivity.class);
        startActivity(mainIntent);
        finish();
    }
}
