package com.example.mo15hammed.firebaseblogapp;

import android.content.Context;
import android.content.Intent;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.QuerySnapshot;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nullable;

public class PostsRecyclerAdapter extends RecyclerView.Adapter<PostsRecyclerAdapter.PostsViewHolder> {

    private static final String TAG = "PostsRecyclerAdapter";

    private Context mContext;
    private List<Post> postsList;
    private List<User> usersList;
    private FirebaseFirestore mFirestoreDatabase;
    private FirebaseAuth mAuth;

    public PostsRecyclerAdapter(Context mContext, List<Post> postsList, List<User> usersList) {
        this.mContext = mContext;
        this.postsList = postsList;
        this.usersList = usersList;
        mFirestoreDatabase = FirebaseFirestore.getInstance();
        mAuth = FirebaseAuth.getInstance();
    }

    @NonNull
    @Override
    public PostsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View mView = LayoutInflater.from(mContext).inflate(R.layout.post_item_layout, viewGroup, false);
        return new PostsViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(@NonNull final PostsViewHolder holder, int position) {

//        holder.setIsRecyclable(false);

        final Post currentPost = postsList.get(position);
        final User currentAuthor = usersList.get(position);
        final String currentPostId = currentPost.getId();
        final String currentUserId = mAuth.getUid();

        long milliseconds = 1;
        if (currentPost.getTimestamp() != null)
            milliseconds = currentPost.getTimestamp().getTime();

        String postDate = new SimpleDateFormat("MMM d, yyyy | h:mm a").format(new Date(milliseconds));

        holder.mPostDate.setText(postDate);

        holder.mPostDesc.setText(currentPost.getDescription());

        final RequestOptions imageOptions = new RequestOptions();
        imageOptions
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background);

        Glide.with(mContext)
                .setDefaultRequestOptions(imageOptions)
                .load(currentPost.getImage()).thumbnail(Glide.with(mContext).load(currentPost.getThumb()))
                .into(holder.mPostImage);

        holder.mUsername.setText(currentAuthor.getName());

        Glide.with(mContext)
            .setDefaultRequestOptions(imageOptions)
            .load(currentAuthor.getThumb())
            .into(holder.mUserImage);

        if (currentUserId != null) {
            mFirestoreDatabase.collection("Posts/" + currentPostId + "/Likes").document(currentUserId).addSnapshotListener(new EventListener<DocumentSnapshot>() {
                @Override
                public void onEvent(@Nullable DocumentSnapshot documentSnapshot, @Nullable FirebaseFirestoreException e) {

                    if (documentSnapshot.exists()) {
                        currentPost.setLiked(true);
                        holder.mLikeImage.setImageResource(R.drawable.ic_like_pressed);

                    } else {
                        currentPost.setLiked(false);
                        holder.mLikeImage.setImageResource(R.drawable.ic_like_default);
                    }
                }
            });
        }

        mFirestoreDatabase.collection("Posts/" + currentPostId + "/Likes").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {

                if (querySnapshot.isEmpty()) {
                    holder.mPostLikesCount.setText("0 Likes");
                } else if (querySnapshot.size() == 1) {
                    holder.mPostLikesCount.setText("1 Like");
                } else {
                    holder.mPostLikesCount.setText(querySnapshot.size() + " Likes");
                }

            }
        });

        mFirestoreDatabase.collection("Posts/" + currentPostId + "/Comments").addSnapshotListener(new EventListener<QuerySnapshot>() {
            @Override
            public void onEvent(@Nullable QuerySnapshot querySnapshot, @Nullable FirebaseFirestoreException e) {

                if (querySnapshot.isEmpty()) {
                    holder.mPostCommentsCount.setText("0 Comments");
                } else if (querySnapshot.size() == 1) {
                    holder.mPostCommentsCount.setText("1 com.example.mo15hammed.firebaseblogapp.Comment");
                } else {
                    holder.mPostCommentsCount.setText(querySnapshot.size() + " Comments");
                }

            }
        });


        holder.mLikeBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentUserId != null) {
                    if (currentPost.isLiked()) {
                        mFirestoreDatabase.collection("Posts/" + currentPostId + "/Likes").document(currentUserId).delete();
                    } else {
                        HashMap<String, Object> likesMap = new HashMap<>();
                        likesMap.put("timestamp", FieldValue.serverTimestamp());
                        mFirestoreDatabase.collection("Posts/" + currentPostId + "/Likes").document(currentUserId).set(likesMap);
                    }
                }
            }
        });

        holder.mCommentBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                if (currentUserId != null) {

                    Intent commentsIntent = new Intent(mContext, CommentsActivity.class);
                    commentsIntent.putExtra("postID", currentPostId);
                    mContext.startActivity(commentsIntent);

                }
            }
        });

    }

    @Override
    public int getItemCount() {
        return postsList.size();
    }

    @Override
    public long getItemId(int position) {
        return position;
    }

    public class PostsViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private ImageView mUserImage, mPostImage, mLikeImage;
        private TextView mUsername, mPostDate, mPostDesc, mPostLikesCount, mPostCommentsCount;
        private LinearLayout mLikeBtn, mCommentBtn;

        public PostsViewHolder(@NonNull View itemView) {
            super(itemView);

            mView = itemView;
            mUserImage = mView.findViewById(R.id.img_user_profile);
            mPostImage = mView.findViewById(R.id.img_post_pic);
            mUsername = mView.findViewById(R.id.txt_user_name);
            mPostDate = mView.findViewById(R.id.txt_post_date);
            mPostDesc = mView.findViewById(R.id.txt_post_desc);
            mLikeImage = mView.findViewById(R.id.img_like_button);
            mLikeBtn = mView.findViewById(R.id.likes_layout);
            mPostLikesCount = mView.findViewById(R.id.txt_likes_count);
            mCommentBtn = mView.findViewById(R.id.comments_layout);
            mPostCommentsCount = mView.findViewById(R.id.txt_comments_count);


        }
    }
}
