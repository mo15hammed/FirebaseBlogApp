package com.example.mo15hammed.firebaseblogapp;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;
import com.theartofdev.edmodo.cropper.CropImage;
import com.theartofdev.edmodo.cropper.CropImageView;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import id.zelory.compressor.Compressor;

public class SetupActivity extends AppCompatActivity {

    private static final String TAG = "SetupActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private boolean isImageChanged = false;

    private FirebaseAuth mAuth;
    private StorageReference mStorageRef;
    private FirebaseFirestore mFirestoreDatabase;

    private Uri profileImageUri, profileThumbUri;

    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private ImageView mProfileImage;
    private EditText mName;
    private Button mSaveChanges;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);

        mAuth = FirebaseAuth.getInstance();
        mFirestoreDatabase = FirebaseFirestore.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();


        mToolbar = findViewById(R.id.setup_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Account Settings");

        mProgressBar = findViewById(R.id.setup_progress_bar);
        mProfileImage = findViewById(R.id.profile_image);
        mName = findViewById(R.id.edt_name);
        mSaveChanges = findViewById(R.id.btn_save_changes);

        loadData();

        mProfileImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                startImageChooserIntent();
            }
        });

        mSaveChanges.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String name = mName.getText().toString();

                if (!TextUtils.isEmpty(name)) {

                    mProgressBar.setVisibility(View.VISIBLE);
                    uploadData(profileImageUri, name);

                } else {
                    Toast.makeText(SetupActivity.this, "Fill Missing Fields !", Toast.LENGTH_SHORT).show();
                }

            }
        });

    }


    private void loadData() {
        String currentUserId = mAuth.getUid();
        mFirestoreDatabase
                .collection("Users")
                .document(currentUserId)
                .get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                if (task.isSuccessful()) {

                    if (task.getResult().exists()) {

                        profileImageUri = Uri.parse(task.getResult().getString("image"));
                        profileThumbUri = Uri.parse(task.getResult().getString("thumb"));
                        String name = task.getResult().getString("name");

                        mName.setText(name);

                        RequestOptions imageOptions = new RequestOptions();
                        imageOptions
                                .placeholder(R.drawable.default_avatar)
                                .error(R.drawable.default_avatar);

                        Glide.with(SetupActivity.this)
                                .setDefaultRequestOptions(imageOptions)
                                .load(profileImageUri)
                                .into(mProfileImage);

                    } else {

                    }

                } else {
                    Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                    Toast.makeText(SetupActivity.this, "(Fetching Data Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }


    /**
     * Don't miss with this one, for it is rather complicated.
     * @param image user profile image
     * @param name user name
     */
    private void uploadData(final Uri image, final String name) {
        final String currentUserId = mAuth.getUid();

        if (isImageChanged) {
            final StorageReference profileImagePath = mStorageRef.child("profile_images").child(currentUserId + ".jpg");
            final StorageReference postCompressedImagePath = mStorageRef.child("profile_images").child("thumbs").child(currentUserId + ".jpg");
            profileImagePath.putFile(image).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                @Override
                public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                    if (task.isSuccessful()) {
                        profileImagePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                            @Override
                            public void onComplete(@NonNull Task<Uri> task) {
                                if (task.isSuccessful()) {
                                    final String imageDownloadUri = task.getResult().toString();
                                    Log.d(TAG, "onSuccess: URL = " + imageDownloadUri);

                                    postCompressedImagePath.putBytes(compressImageToBytes(image)).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
                                        @Override
                                        public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                                            if (task.isSuccessful()) {
                                                postCompressedImagePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
                                                    @Override
                                                    public void onComplete(@NonNull Task<Uri> task) {
                                                        if (task.isSuccessful()) {

                                                            String thumbDownloadUri = task.getResult().toString();
                                                            Log.d(TAG, "onSuccess: URL = " + thumbDownloadUri);

                                                            storeFirestore(name, imageDownloadUri, thumbDownloadUri);

                                                        } else {
                                                            mProgressBar.setVisibility(View.INVISIBLE);
                                                            Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                                                            Toast.makeText(SetupActivity.this, "(Thumb Download URL Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                                                        }
                                                    }
                                                });
                                            } else {
                                                mProgressBar.setVisibility(View.INVISIBLE);
                                                Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                                                Toast.makeText(SetupActivity.this, "(Upload Thumb) : " + task.getException(), Toast.LENGTH_SHORT).show();
                                            }
                                        }
                                    });


                                } else {
                                    mProgressBar.setVisibility(View.INVISIBLE);
                                    Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                                    Toast.makeText(SetupActivity.this, "(Download URL Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                                }
                            }
                        });

                    } else {
                        mProgressBar.setVisibility(View.INVISIBLE);
                        Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                        Toast.makeText(SetupActivity.this, "(Upload IMAGE) : " + task.getException(), Toast.LENGTH_SHORT).show();

                    }
                }
            });
        } else {
            if (profileImageUri != null)
                storeFirestore(name, profileImageUri.toString(), profileThumbUri.toString());
            else
                storeFirestore(name, "default_avatar", "default_avatar");
        }
    }


    private void storeFirestore(String name, String image, String thumb) {
        final String currentUserId = mAuth.getUid();

        Map<String, String> userMap = new HashMap<>();
        userMap.put("name", name);
        userMap.put("image", image);
        userMap.put("thumb", thumb);

        mFirestoreDatabase
                .collection("Users")
                .document(currentUserId)
                .set(userMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mProgressBar.setVisibility(View.INVISIBLE);
                if (task.isSuccessful()) {

                    Toast.makeText(SetupActivity.this, "Added", Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(SetupActivity.this, MainActivity.class);
                    startActivity(mainIntent);
                    finish();

                } else {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                    Toast.makeText(SetupActivity.this, "(Update Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    /**
     * checks Storage Permission, asks user for it if not granted, and start Image Cropper Intent
     */
    private void startImageChooserIntent() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(SetupActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            Toast.makeText(SetupActivity.this, "NOT GRANTED", Toast.LENGTH_SHORT).show();

            // No explanation needed, we can request the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(SetupActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                startImageCropperIntent();
            }

        } else {
            // Permission has already been granted
            Toast.makeText(SetupActivity.this, "GRANTED", Toast.LENGTH_SHORT).show();

            // start picker to get image for cropping and then use the image in cropping activity
            startImageCropperIntent();
        }

    }

    private void startImageCropperIntent() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setAspectRatio(1, 1)
                .start(SetupActivity.this);
    }


    private byte[] compressImageToBytes(Uri image) {

        File imageFile = new File(image.getPath());
        Bitmap compressedImageBitmap = null;
        try {
            compressedImageBitmap = new Compressor(this)
                    .setMaxWidth(50)
                    .setMaxHeight(50)
                    .setQuality(50)
                    .compressToBitmap(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 50, baos);
        byte[] data = baos.toByteArray();


        return data;
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {

        switch (requestCode) {

            case PERMISSION_REQUEST_CODE:

                // If request is cancelled, the result arrays are empty.
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {

                    // start picker to get image for cropping and then use the image in cropping activity
                    startImageCropperIntent();

                } else { // Permission denied
                    Toast.makeText(SetupActivity.this, "DENIED", Toast.LENGTH_SHORT).show();
                }

                return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                isImageChanged = true;
                profileImageUri = result.getUri();
                mProfileImage.setImageURI(profileImageUri);

            } else if (resultCode == CropImage.CROP_IMAGE_ACTIVITY_RESULT_ERROR_CODE) {
                Exception error = result.getError();
                Log.d(TAG, "onActivityResult: Error : " + error);
                Toast.makeText(this, "error", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            sendToLogin();
        }

    }

    private void sendToLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }
}
