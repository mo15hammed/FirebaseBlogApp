package com.example.mo15hammed.firebaseblogapp;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.Toolbar;
import android.text.Editable;
import android.text.TextUtils;
import android.text.TextWatcher;
import android.util.Log;
import android.view.MenuItem;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

public class CommentsActivity extends AppCompatActivity {

    private static final String TAG = "CommentsActivity";

    private FirebaseAuth mAuth;
    private FirebaseFirestore mFirestoreDatabase;

    private Toolbar mToolbar;
    private EditText mComment;
    private ImageView mSendComment;
    private RecyclerView mCommentRecycler;

    private CommentRecyclerAdapter mAdapter;
    private List<Comment> commentsList;

    private String curentUserId;
    private String currentPostId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_comments);

        mAuth = FirebaseAuth.getInstance();
        mFirestoreDatabase = FirebaseFirestore.getInstance();
        currentPostId = getIntent().getStringExtra("postID");
        if (mAuth.getCurrentUser() != null)
            curentUserId = mAuth.getCurrentUser().getUid();

        commentsList = new ArrayList<>();

        mToolbar = findViewById(R.id.comments_app_bar);
        setSupportActionBar(mToolbar);
        getSupportActionBar().setTitle("Comments");
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mComment = findViewById(R.id.edt_comment);
        mSendComment = findViewById(R.id.btn_send);
        mSendComment.setEnabled(false);

        mCommentRecycler = findViewById(R.id.comments_recycler);
        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(this);
        linearLayoutManager.setStackFromEnd(true);
        mCommentRecycler.setLayoutManager(linearLayoutManager);
        mCommentRecycler.setHasFixedSize(true);
        mAdapter = new CommentRecyclerAdapter(this, commentsList);
        mCommentRecycler.setAdapter(mAdapter);

        loadComments();

        mSendComment.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                sendComment();

            }
        });


        mComment.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (!TextUtils.isEmpty(s)) {
                    mSendComment.setEnabled(true);
                    mSendComment.setImageResource(R.drawable.ic_send_enabled);
                } else {
                    mSendComment.setEnabled(false);
                    mSendComment.setImageResource(R.drawable.ic_send_default);
                    
                }
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

    }

    private void sendComment() {

        Map<String, Object> commentMap = new HashMap<>();
        commentMap.put("userID", curentUserId);
        commentMap.put("comment", mComment.getText().toString());
        commentMap.put("timestamp", System.currentTimeMillis());


        mFirestoreDatabase.collection("Posts/" + currentPostId + "/Comments").add(commentMap).addOnCompleteListener(new OnCompleteListener<DocumentReference>() {
            @Override
            public void onComplete(@NonNull Task<DocumentReference> task) {

                if (task.isSuccessful()) {

                    mComment.setText("");
                    Toast.makeText(CommentsActivity.this, "Commented", Toast.LENGTH_SHORT).show();
                    mCommentRecycler.scrollToPosition(mAdapter.getItemCount() - 1);

                } else {
                    Log.d(TAG, "onComplete: Exception = " + task.getException());
                    Toast.makeText(CommentsActivity.this, "Error: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }


    private void loadComments() {

        mFirestoreDatabase.collection("Posts/" + currentPostId + "/Comments").orderBy("timestamp").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {

                if (querySnapshot != null) {
                    if (!querySnapshot.isEmpty()) {

                        for (DocumentChange doc : querySnapshot.getDocumentChanges()) {
                            if (doc.getType() == DocumentChange.Type.ADDED) {

                                final Comment comment = doc.getDocument().toObject(Comment.class);
                                String authorId = comment.getUserID();

                                mFirestoreDatabase.collection("Users").document(authorId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                    @Override
                                    public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                        if (task.isSuccessful()) {

                                            User author = task.getResult().toObject(User.class);
                                            comment.setAuthor(author);
                                            commentsList.add(comment);

                                            mAdapter.notifyDataSetChanged();

                                            Log.d(TAG, "onEvent: COMMENTS = " + mAdapter.getItemCount());

                                        } else {
                                            Log.d(TAG, "onComplete: Exception = " + task.getException().getMessage());
                                        }

                                    }
                                });

                            }
                        }

                    }
                }

            }
        });

    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
