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
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.MultiAutoCompleteTextView;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.FieldValue;
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

public class NewPostActivity extends AppCompatActivity {

    private static final String TAG = "NewPostActivity";
    private static final int PERMISSION_REQUEST_CODE = 100;

    private boolean isPosting = false;

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestoreDatabase;
    private StorageReference mStorageRef;

    private Uri postImageUri;

    private Toolbar mToolbar;
    private ProgressBar mProgressBar;
    private ImageView mPostImage;
    private MultiAutoCompleteTextView mPostDescription;
    private Button mCreatePost;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_post);

        mAuth = FirebaseAuth.getInstance();
        mStorageRef = FirebaseStorage.getInstance().getReference();
        mFirestoreDatabase = FirebaseFirestore.getInstance();

        mToolbar = findViewById(R.id.new_post_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Add New Post");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mProgressBar = findViewById(R.id.new_post_progress_bar);
        mPostImage = findViewById(R.id.img_new_post);
        mPostDescription = findViewById(R.id.edt_description);
        mCreatePost = findViewById(R.id.btn_add_post);


        mPostImage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startImageChooserIntent();
            }
        });

        mCreatePost.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String desc = mPostDescription.getText().toString();
                if (!TextUtils.isEmpty(desc) && postImageUri != null) {
                    mProgressBar.setVisibility(View.VISIBLE);

                    if (!isPosting)
                        createPost(postImageUri, desc);
                    else
                        Toast.makeText(NewPostActivity.this, "Posting !!!", Toast.LENGTH_SHORT).show();

                } else {

                    Toast.makeText(NewPostActivity.this, "Image And Description Are Both Needed !", Toast.LENGTH_SHORT).show();

                }

            }
        });

    }

    private void createPost(final Uri image, final String description) {
        isPosting = true;
        String randomImageName = String.valueOf(System.currentTimeMillis());

        final StorageReference postImagePath = mStorageRef.child("posts_images").child(randomImageName + ".jpg");
        final StorageReference postCompressedImagePath = mStorageRef.child("posts_images").child("thumbs").child(randomImageName + ".jpg");

        postImagePath.putFile(image).addOnCompleteListener(new OnCompleteListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onComplete(@NonNull Task<UploadTask.TaskSnapshot> task) {
                if (task.isSuccessful()) {
                    postImagePath.getDownloadUrl().addOnCompleteListener(new OnCompleteListener<Uri>() {
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

                                                        storeFirestore(description, imageDownloadUri, thumbDownloadUri);

                                                    } else {
                                                        mProgressBar.setVisibility(View.INVISIBLE);
                                                        Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                                                        Toast.makeText(NewPostActivity.this, "(Thumb Download URL Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                                                    }
                                                }
                                            });
                                        } else {
                                            mProgressBar.setVisibility(View.INVISIBLE);
                                            Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                                            Toast.makeText(NewPostActivity.this, "(Upload Thumb) : " + task.getException(), Toast.LENGTH_SHORT).show();
                                        }
                                    }
                                });


                            } else {
                                mProgressBar.setVisibility(View.INVISIBLE);
                                Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                                Toast.makeText(NewPostActivity.this, "(Download URL Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                            }
                        }
                    });

                } else {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                    Toast.makeText(NewPostActivity.this, "(Upload Image) : " + task.getException(), Toast.LENGTH_SHORT).show();

                }
            }
        });
    }

    private void storeFirestore(String description, String image, String thumb) {
        final String currentUserId = mAuth.getUid();

        Map<String, Object> postMap = new HashMap<>();
        postMap.put("timestamp", FieldValue.serverTimestamp());
        postMap.put("description", description);
        postMap.put("image", image);
        postMap.put("thumb", thumb);
        postMap.put("authorID", currentUserId);

        mFirestoreDatabase
                .collection("Posts")
                .document()
                .set(postMap).addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                mProgressBar.setVisibility(View.INVISIBLE);
                if (task.isSuccessful()) {

                    isPosting = false;
                    Toast.makeText(NewPostActivity.this, "Posted", Toast.LENGTH_SHORT).show();
                    Intent mainIntent = new Intent(NewPostActivity.this, MainActivity.class);
                    mainIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(mainIntent);
                    finish();

                } else {
                    mProgressBar.setVisibility(View.INVISIBLE);
                    Log.d(TAG, "onFailure: Exception : " + task.getException().getMessage());
                    Toast.makeText(NewPostActivity.this, "(Update Failed) : " + task.getException(), Toast.LENGTH_SHORT).show();
                }
            }
        });

    }




    /**
     * checks Storage Permission, asks user for it if not granted, and start Image Cropper Intent
     */
    private void startImageChooserIntent() {
        // Here, thisActivity is the current activity
        if (ContextCompat.checkSelfPermission(NewPostActivity.this,
                Manifest.permission.READ_EXTERNAL_STORAGE)
                != PackageManager.PERMISSION_GRANTED) {

            // No explanation needed, we can request the permission.
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.JELLY_BEAN) {
                ActivityCompat.requestPermissions(NewPostActivity.this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE},
                        PERMISSION_REQUEST_CODE);
            } else {
                startImageCropperIntent();
            }

        } else {
            // Permission has already been granted
            // start picker to get image for cropping and then use the image in cropping activity
            startImageCropperIntent();
        }

    }

    private void startImageCropperIntent() {
        CropImage.activity()
                .setGuidelines(CropImageView.Guidelines.ON)
                .setMinCropResultSize(512, 512)
                .start(NewPostActivity.this);
    }


    private byte[] compressImageToBytes(Uri image) {

        File imageFile = new File(image.getPath());
        Bitmap compressedImageBitmap = null;
        try {
            compressedImageBitmap = new Compressor(this)
                    .setMaxWidth(200)
                    .setMaxHeight(100)
                    .setQuality(75)
                    .compressToBitmap(imageFile);
        } catch (IOException e) {
            e.printStackTrace();
        }


        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        compressedImageBitmap.compress(Bitmap.CompressFormat.JPEG, 75, baos);
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
                    Toast.makeText(NewPostActivity.this, "DENIED", Toast.LENGTH_SHORT).show();
                }

                return;
        }

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {

        if (requestCode == CropImage.CROP_IMAGE_ACTIVITY_REQUEST_CODE) {
            CropImage.ActivityResult result = CropImage.getActivityResult(data);
            if (resultCode == RESULT_OK) {

                postImageUri = result.getUri();
                mPostImage.setImageURI(postImageUri);

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

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return false;
    }

    private void sendToLogin() {
        Intent loginIntent = new Intent(this, LoginActivity.class);
        loginIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(loginIntent);
    }
}
