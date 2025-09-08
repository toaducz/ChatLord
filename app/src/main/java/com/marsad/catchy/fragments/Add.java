package com.marsad.catchy.fragments;

import android.Manifest;
import android.app.Dialog;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.karumi.dexter.Dexter;
import com.karumi.dexter.MultiplePermissionsReport;
import com.karumi.dexter.PermissionToken;
import com.karumi.dexter.listener.PermissionRequest;
import com.karumi.dexter.listener.multi.MultiplePermissionsListener;
import com.marsad.catchy.R;
import com.marsad.catchy.adapter.GalleryAdapter;
import com.marsad.catchy.model.GalleryImages;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Add extends Fragment {

    private static final String TAG = "AddFragment";

    Uri imageUri;
    Dialog dialog;

    private EditText descET;
    private ImageView imageView;
    private RecyclerView recyclerView;
    private ImageButton backBtn, nextBtn;
    private List<GalleryImages> list;
    private GalleryAdapter adapter;
    private FirebaseUser user;
    private ProgressBar progressBar; // Ensure this is initialized if used

    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    public Add() {
        // Required empty public constructor
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_add, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        init(view);

        final Context localContext = getContext();
        if (localContext != null && isAdded()) {
            recyclerView.setLayoutManager(new GridLayoutManager(localContext, 3));
            recyclerView.setHasFixedSize(true);

            list = new ArrayList<>();
            adapter = new GalleryAdapter(list);
            recyclerView.setAdapter(adapter);

            adapter.setOnImageSelectedListener(picUri -> {
                this.imageUri = picUri;
                if (getContext() != null && isAdded()) { // Re-check context for Glide
                    Glide.with(getContext()).load(picUri).into(imageView);
                }
                imageView.setVisibility(View.VISIBLE);
                nextBtn.setVisibility(View.VISIBLE);
            });
        }
        clickListener();
    }

    private void init(View view) {
        descET = view.findViewById(R.id.descriptionET);
        imageView = view.findViewById(R.id.imageView);
        recyclerView = view.findViewById(R.id.recyclerView);
        backBtn = view.findViewById(R.id.backBtn);
        nextBtn = view.findViewById(R.id.nextBtn);
        // progressBar = view.findViewById(R.id.progressBar); // Initialize if you have a ProgressBar

        user = FirebaseAuth.getInstance().getCurrentUser();

        final Context localContext = getContext();
        if (localContext != null && isAdded()) {
            dialog = new Dialog(localContext);
            dialog.setContentView(R.layout.laoding_dialog);
            if (dialog.getWindow() != null) {
                try {
                    dialog.getWindow().setBackgroundDrawable(ResourcesCompat.getDrawable(localContext.getResources(), R.drawable.dialog_bg, null));
                } catch (Exception e) {
                    Log.e(TAG, "Error setting dialog background: " + e.getMessage());
                }
            }
            dialog.setCancelable(false);
        }
    }

    private void clickListener() {
        nextBtn.setOnClickListener(v -> {
            final Context localContext = getContext();
            if (localContext == null || !isAdded()) {
                return;
            }

            if (imageUri == null) {
                Toast.makeText(localContext, "Vui lòng chọn một ảnh", Toast.LENGTH_SHORT).show();
                return;
            }

            if (dialog == null) {
                // Attempt to re-initialize dialog if it's null and context is valid
                dialog = new Dialog(localContext);
                dialog.setContentView(R.layout.laoding_dialog);
                if (dialog.getWindow() != null) {
                    try {
                        dialog.getWindow().setBackgroundDrawable(ResourcesCompat.getDrawable(localContext.getResources(), R.drawable.dialog_bg, null));
                    } catch (Exception e) {
                        Log.e(TAG, "Error setting dialog background in clickListener: " + e.getMessage());
                    }
                }
                dialog.setCancelable(false);
            }

            dialog.show();

            Log.d(TAG, "Uploading image: " + imageUri.toString());
            FirebaseStorage storage = FirebaseStorage.getInstance();
            final StorageReference storageReference = storage.getReference().child("Post Images/" + System.currentTimeMillis());
            Log.d(TAG, "Storage path: " + storageReference.getPath());

            storageReference.putFile(imageUri)
                .addOnSuccessListener(taskSnapshot -> {
                    Log.d(TAG, "putFile successful");
                    storageReference.getDownloadUrl().addOnSuccessListener(uri -> {
                        Log.d(TAG, "getDownloadUrl successful: " + uri.toString());
                        if (isAdded()) { // Check fragment state before proceeding
                            uploadData(uri.toString());
                        }
                    }).addOnFailureListener(e -> {
                        Log.e(TAG, "getDownloadUrl failed: " + e.getMessage(), e);
                        if (isAdded()) {
                            if (dialog != null && dialog.isShowing()) dialog.dismiss();
                            Toast.makeText(localContext, "Lấy URL tải xuống thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "putFile failed: " + e.getMessage(), e);
                    if (isAdded()) {
                        if (dialog != null && dialog.isShowing()) dialog.dismiss();
                        Toast.makeText(localContext, "Tải ảnh lên thất bại: " + e.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
        });

        backBtn.setOnClickListener(v -> {
            imageView.setVisibility(View.GONE);
            nextBtn.setVisibility(View.GONE);
            imageUri = null;
        });
    }

    private void uploadData(String imageURL) {
        final Context localContext = getContext();
        if (user == null || localContext == null || !isAdded()) {
            if (dialog != null && dialog.isShowing()) dialog.dismiss();
            return;
        }

        CollectionReference reference = FirebaseFirestore.getInstance().collection("Users")
                .document(user.getUid()).collection("Post Images");

        String id = reference.document().getId();
        String description = descET.getText().toString();
        List<String> likesList = new ArrayList<>();

        Map<String, Object> map = new HashMap<>();
        map.put("id", id);
        map.put("description", description);
        map.put("imageUrl", imageURL);
        map.put("timestamp", FieldValue.serverTimestamp());

        if (user.getDisplayName() != null) map.put("name", user.getDisplayName());
        if (user.getPhotoUrl() != null) map.put("profileImage", String.valueOf(user.getPhotoUrl()));
        else map.put("profileImage", ""); // Default or empty if no photo
        
        map.put("likes", likesList);
        map.put("uid", user.getUid());

        reference.document(id).set(map)
            .addOnCompleteListener(task -> {
                if (isAdded()) { // Check fragment state
                    if (task.isSuccessful()) {
                        Toast.makeText(localContext, "Đã đăng bài", Toast.LENGTH_SHORT).show();
                        descET.setText("");
                        imageView.setVisibility(View.GONE);
                        nextBtn.setVisibility(View.GONE);
                        imageUri = null;
                    } else {
                        String errorMessage = task.getException() != null ? task.getException().getMessage() : "Lỗi không xác định";
                        Toast.makeText(localContext, "Lỗi khi đăng bài: " + errorMessage, Toast.LENGTH_SHORT).show();
                        Log.e(TAG, "Error posting data: " + errorMessage, task.getException());
                    }
                    if (dialog != null && dialog.isShowing()) dialog.dismiss();
                }
            });
    }

    @Override
    public void onResume() {
        super.onResume();
        checkPermissionsAndLoadImages();
    }

    private void checkPermissionsAndLoadImages() {
        final Context localContext = getContext();
        if (localContext == null || !isAdded()) return;

        String[] permissionsToRequest;
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            permissionsToRequest = new String[]{Manifest.permission.READ_MEDIA_IMAGES};
        } else {
            permissionsToRequest = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        }

        Dexter.withContext(localContext)
            .withPermissions(permissionsToRequest)
            .withListener(new MultiplePermissionsListener() {
                @Override
                public void onPermissionsChecked(MultiplePermissionsReport report) {
                    if (isAdded()) { // Check fragment state
                        if (report.areAllPermissionsGranted()) {
                            loadImagesFromDeviceAsync();
                        } else {
                            Toast.makeText(localContext, "Cần cấp quyền để tải ảnh", Toast.LENGTH_SHORT).show();
                        }
                    }
                }

                @Override
                public void onPermissionRationaleShouldBeShown(List<PermissionRequest> permissionRequests, PermissionToken permissionToken) {
                    permissionToken.continuePermissionRequest();
                }
            }).check();
    }

    private void loadImagesFromDeviceAsync() {
        final Context safeContext = getContext();
        final FragmentActivity safeActivity = getActivity();

        if (safeContext == null || safeActivity == null || !isAdded()) {
            return;
        }

        executorService.execute(() -> {
            final List<GalleryImages> loadedImages = new ArrayList<>();
            Uri collection;

            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                collection = MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL);
            } else {
                collection = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
            }

            String[] projection = new String[]{
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            };
            String sortOrder = MediaStore.Images.Media.DATE_TAKEN + " DESC";

            try (Cursor cursor = safeContext.getContentResolver().query(
                collection, projection, null, null, sortOrder
            )) {
                if (cursor != null) {
                    int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
                    while (cursor.moveToNext()) {
                        long id = cursor.getLong(idColumn);
                        Uri contentUri = ContentUris.withAppendedId(
                            MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);
                        loadedImages.add(new GalleryImages(contentUri));
                    }
                }
            } catch (Exception e) {
                Log.e(TAG, "Error loading images from device: " + e.getMessage(), e);
            }

            if (safeActivity != null && isAdded()) {
                safeActivity.runOnUiThread(() -> {
                    if (!isAdded()) return;
                    list.clear();
                    list.addAll(loadedImages);
                    adapter.notifyDataSetChanged();
                    if (loadedImages.isEmpty()) {
                         // Check context again before showing toast
                        Context currentContext = getContext(); 
                        if (currentContext != null) {
                           Toast.makeText(currentContext, "Không tìm thấy ảnh nào trong thư viện", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (dialog != null) {
            if (dialog.isShowing()) {
                dialog.dismiss();
            }
            dialog = null;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executorService != null && !executorService.isShutdown()) {
            executorService.shutdownNow();
        }
    }
}
