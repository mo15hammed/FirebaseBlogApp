package com.example.mo15hammed.firebaseblogapp;

import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.RequestOptions;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;

public class CommentRecyclerAdapter extends RecyclerView.Adapter<CommentRecyclerAdapter.CommentsViewHolder> {
    private static final String TAG = "CommentRecyclerAdapter";

    private Context mContext;
    private List<Comment> commentsList;
    private FirebaseFirestore mFirestoreDatabase;


    CommentRecyclerAdapter(Context mContext, List<Comment> commentsList) {
        this.mContext = mContext;
        this.commentsList = commentsList;
        mFirestoreDatabase = FirebaseFirestore.getInstance();
    }

    @NonNull
    @Override
    public CommentsViewHolder onCreateViewHolder(@NonNull ViewGroup viewGroup, int i) {
        View mView = LayoutInflater.from(mContext).inflate(R.layout.comment_item_layout, viewGroup, false);
        return new CommentsViewHolder(mView);
    }

    @Override
    public void onBindViewHolder(@NonNull final CommentsViewHolder holder, int position) {

        Comment currentComment = commentsList.get(position);

        holder.mComment.setText(currentComment.getComment());

        String commentDate = new SimpleDateFormat("MMM d, yyyy | h:mm a").format(new Date(currentComment.getTimestamp()));
        holder.mCommentDate.setText(commentDate);

        final RequestOptions imageOptions = new RequestOptions();
        imageOptions
                .placeholder(R.drawable.default_background)
                .error(R.drawable.default_background);

        mFirestoreDatabase
                .collection("Users")
                .document(currentComment.getUserID())
                .get().addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
            @Override
            public void onSuccess(DocumentSnapshot documentSnapshot) {

                if (documentSnapshot.exists()) {
                    holder.mUsername.setText(documentSnapshot.getString("name"));

                    Glide.with(mContext)
                            .setDefaultRequestOptions(imageOptions)
                            .load(documentSnapshot.getString("thumb"))
                            .into(holder.mUserImage);
                } else {
                    Log.d(TAG, "onSuccess: User Does not exist !!");
                }
            }
        }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.d(TAG, "onFailure: Exception : " + e.getMessage());
            }
        });

    }

    @Override
    public int getItemCount() {
        return commentsList.size();
    }

    public class CommentsViewHolder extends RecyclerView.ViewHolder {

        private View mView;
        private ImageView mUserImage;
        private TextView mUsername, mComment, mCommentDate;

        public CommentsViewHolder(@NonNull View itemView) {
            super(itemView);
            mView = itemView;
            mUserImage = mView.findViewById(R.id.img_user_profile);
            mUsername = mView.findViewById(R.id.txt_user_name);
            mComment = mView.findViewById(R.id.txt_comment);
            mCommentDate = mView.findViewById(R.id.txt_comment_date);

        }
    }

}
