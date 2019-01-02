package com.example.mo15hammed.firebaseblogapp;

import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentChange;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private static final int POSTS_PER_PAGE = 3;

    private static final int FIRST_LOAD = 1;
    private static final int MORE_LOAD = 2;
    private boolean isFirstLoad = true;


    private FirebaseFirestore mFirestoreDatabase;
    private Context mContext;

    private ProgressBar mProgressBar;
    private TextView mNoPostsFound;
    private SwipeRefreshLayout mRefresh;
    private RecyclerView mPostsRecycler;
    private PostsRecyclerAdapter mPostsRecyclerAdapter;
    private List<Post> postsList;
    private List<User> usersList;

    private DocumentSnapshot lastVisibleDocument;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment

        View mView = inflater.inflate(R.layout.fragment_home, container, false);

        mContext = getContext();
        mFirestoreDatabase = FirebaseFirestore.getInstance();

        mProgressBar = mView.findViewById(R.id.home_progress_bar);
        mNoPostsFound = mView.findViewById(R.id.txt_no_posts);
        mRefresh = mView.findViewById(R.id.refresh_posts);
        
        mPostsRecycler = mView.findViewById(R.id.posts_recycler);
        postsList = new ArrayList<>();
        usersList = new ArrayList<>();

        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(mContext);
        mPostsRecycler.setLayoutManager(linearLayoutManager);
        mPostsRecycler.setHasFixedSize(true);

        mPostsRecyclerAdapter = new PostsRecyclerAdapter(mContext, postsList, usersList);

        mPostsRecycler.setAdapter(mPostsRecyclerAdapter);

        loadFirstPage();
        
        mRefresh.setOnRefreshListener(new SwipeRefreshLayout.OnRefreshListener() {
            @Override
            public void onRefresh() {

                resetActivity();

            }
        });
        
        mPostsRecycler.setOnScrollChangeListener(new View.OnScrollChangeListener() {
            @Override
            public void onScrollChange(View v, int scrollX, int scrollY, int oldScrollX, int oldScrollY) {

                boolean isLastReached = !mPostsRecycler.canScrollVertically(1);

                if (isLastReached) {

                    loadMorePage();

                }


            }
        });

        return mView;
    }


    private void loadFirstPage() {

        mProgressBar.setVisibility(View.VISIBLE);
        Query query = mFirestoreDatabase.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .limit(POSTS_PER_PAGE);

        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if(task.isSuccessful()) {
                    if (!task.getResult().isEmpty()) {

                        Log.d(TAG, "onComplete: #TESTING: Empty = " + task.getResult().isEmpty());

                        lastVisibleDocument = task.getResult().getDocuments().get(task.getResult().size() - 1);

                        Log.d(TAG, "#_TESTING: lastVisibleDocument = " + lastVisibleDocument.toObject(Post.class).getDescription());

                        for (DocumentChange doc : task.getResult().getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {

                                Log.d(TAG, "#_TESTING: DOC = " + doc.getDocument().toObject(Post.class).getDescription());
                                Log.d(TAG, "#_TESTING: FOR");

                                String docId = doc.getDocument().getId();
                                final Post post = doc.getDocument().toObject(Post.class).withId(docId);

                                postsList.add(post);

                            }
                        }

                        for (int i = 0; i < postsList.size(); i++) {

                            String authorId = postsList.get(i).getAuthorID();

                            mFirestoreDatabase.collection("Users").document(authorId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {

                                        Log.d(TAG, "#_TESTING: GET");

                                        User author = task.getResult().toObject(User.class);
                                        usersList.add(author);

                                        mPostsRecyclerAdapter.notifyDataSetChanged();
                                        mProgressBar.setVisibility(View.GONE);

                                        Log.d(TAG, "onEvent: POSTS = " + mPostsRecyclerAdapter.getItemCount());
                                    } else {
                                        Log.d(TAG, "onComplete: Exception = " + task.getException().getMessage());
                                    }

                                }
                            });

                        }

                    } else {
                        if (postsList.size() <= 0) {
                            mNoPostsFound.setVisibility(View.VISIBLE);
                            mPostsRecyclerAdapter.notifyDataSetChanged();
                        }
                        mProgressBar.setVisibility(View.GONE);
                    }
                }

            }
        });
    }

    private void loadMorePage() {
        mProgressBar.setVisibility(View.VISIBLE);
        Query query = mFirestoreDatabase.collection("Posts")
                .orderBy("timestamp", Query.Direction.DESCENDING)
                .startAfter(lastVisibleDocument)
                .limit(POSTS_PER_PAGE);


        query.get().addOnCompleteListener(new OnCompleteListener<QuerySnapshot>() {
            @Override
            public void onComplete(@NonNull Task<QuerySnapshot> task) {
                if (task.isSuccessful()) {
                    if(!task.getResult().isEmpty()) {

                        // Get the last visible document
                        lastVisibleDocument = task.getResult().getDocuments().get(task.getResult().size() - 1);

                        Log.d(TAG, "#_TESTING: lastVisibleDocument = " + lastVisibleDocument.toObject(Post.class).getDescription());

                        for (DocumentChange doc : task.getResult().getDocumentChanges()) {

                            if (doc.getType() == DocumentChange.Type.ADDED) {

                                Log.d(TAG, "#_TESTING: DOC = " + doc.getDocument().toObject(Post.class).getDescription());

                                String docId = doc.getDocument().getId();
                                final Post post = doc.getDocument().toObject(Post.class).withId(docId);
//                                String authorId = post.getAuthorID();

                                postsList.add(post);

                            }

                        }

                        for (int i = 0; i < postsList.size(); i++) {
                            String authorId = postsList.get(i).getAuthorID();

                            mFirestoreDatabase.collection("Users").document(authorId).get().addOnCompleteListener(new OnCompleteListener<DocumentSnapshot>() {
                                @Override
                                public void onComplete(@NonNull Task<DocumentSnapshot> task) {
                                    if (task.isSuccessful()) {

                                        User author = task.getResult().toObject(User.class);

                                        usersList.add(author);
                                        mPostsRecyclerAdapter.notifyDataSetChanged();

                                        mProgressBar.setVisibility(View.GONE);

                                        Log.d(TAG, "onEvent: POSTS = " + mPostsRecyclerAdapter.getItemCount());

                                    } else {
                                        Log.d(TAG, "onComplete: Exception = " + task.getException().getMessage());
                                    }

                                }
                            });
                        }

                    } else {
                        if (postsList.size() <= 0) {
                            mNoPostsFound.setVisibility(View.VISIBLE);
                            mPostsRecyclerAdapter.notifyDataSetChanged();
                        }
                        mProgressBar.setVisibility(View.GONE);
                    }
                }
            }
        });
    }



    private void resetActivity() {

        postsList.clear();
        usersList.clear();
        loadFirstPage();
        mNoPostsFound.setVisibility(View.GONE);
        if (mRefresh.isRefreshing())
            mRefresh.setRefreshing(false);
    }

}
