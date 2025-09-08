package com.marsad.catchy.fragments;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.QueryDocumentSnapshot;
import com.marsad.catchy.R;
import com.marsad.catchy.adapter.HomeAdapter;
import com.marsad.catchy.model.HomeModel;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class HomeFragment extends Fragment implements HomeAdapter.OnPressed {

    private RecyclerView recyclerView;
    private HomeAdapter homeAdapter;
    private List<HomeModel> homeModelList;
    private FirebaseUser firebaseUser;

    // TODO: Consider adding a ProgressBar for loading state

    public HomeFragment() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        firebaseUser = FirebaseAuth.getInstance().getCurrentUser();
        initRecyclerView(view);
        fetchPosts();
    }

    private void initRecyclerView(View view) {
        recyclerView = view.findViewById(R.id.recyclerView); // Make sure you have recyclerView in fragment_home.xml
        recyclerView.setHasFixedSize(true);
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));

        homeModelList = new ArrayList<>();
        homeAdapter = new HomeAdapter(homeModelList, getActivity());
        homeAdapter.OnPressed(this); // Set the listener
        recyclerView.setAdapter(homeAdapter);
    }

    private void fetchPosts() {
        if (firebaseUser == null) {
            // Handle user not logged in case
            Toast.makeText(getContext(), "User not logged in", Toast.LENGTH_SHORT).show();
            return;
        }

        CollectionReference postsRef = FirebaseFirestore.getInstance().collection("Posts");
        // Consider ordering posts by timestamp
        postsRef.orderBy("timestamp", Query.Direction.DESCENDING) // Assuming you have a 'timestamp' field
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        homeModelList.clear();
                        for (QueryDocumentSnapshot document : task.getResult()) {
                            HomeModel homeModel = document.toObject(HomeModel.class);
                            homeModelList.add(homeModel);
                        }
                        homeAdapter.notifyDataSetChanged();
                        // TODO: Hide ProgressBar here
                    } else {
                        Toast.makeText(getContext(), "Error fetching posts: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                        // TODO: Hide ProgressBar here
                    }
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Failed to fetch posts: " + e.getMessage(), Toast.LENGTH_LONG).show();
                     // TODO: Hide ProgressBar here
                });
    }

    @Override
    public void onLiked(int position, String id, String uid, List<String> likeList, boolean isChecked) {

        DocumentReference postRef = FirebaseFirestore.getInstance()
                .collection("Posts")
                .document(id);

        if (firebaseUser == null) return;

        String currentUserId = firebaseUser.getUid();

        if (isChecked) {
            if (!likeList.contains(currentUserId)) {
                likeList.add(currentUserId);
            }
        } else {
            likeList.remove(currentUserId);
        }

        Map<String, Object> updates = new HashMap<>();
        updates.put("likes", likeList);

        postRef.update(updates).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Optionally update the UI or model directly if needed,
                // but notifyItemChanged should handle it based on adapter logic
                homeAdapter.notifyItemChanged(position);
            } else {
                // Revert UI change if Firestore update fails
                if (isChecked) {
                    likeList.remove(currentUserId); // Revert add
                } else {
                    likeList.add(currentUserId); // Revert remove
                }
                homeAdapter.notifyItemChanged(position); // Notify to revert checkbox state
                Toast.makeText(getContext(), "Like update failed", Toast.LENGTH_SHORT).show();
            }
        });
    }

    @Override
    public void setCommentCount(TextView textView) {
        // This method is called from HomeAdapter's HomeHolder.
        // You might want to query the actual comment count for a post here
        // and then update the textView.
        // For now, let's assume it's handled or will be implemented.
        // Example: textView.setText("10 Comments");
        // This needs a post ID to fetch comments for, which is not directly available here.
        // Consider moving comment count logic or making post ID available.
    }

    // It's good practice to clear listeners or resources in onDestroyView if necessary
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Clear any listeners or heavy objects here if they won't be automatically garbage collected
        // e.g., if you had Firestore listeners:
        // if (firestoreListenerRegistration != null) {
        // firestoreListenerRegistration.remove();
        // }
        if (homeModelList != null) {
            homeModelList.clear(); // Clear the list to free up memory
        }
        if (recyclerView != null) {
            recyclerView.setAdapter(null); // Help GC by removing adapter reference
        }
    }
}
