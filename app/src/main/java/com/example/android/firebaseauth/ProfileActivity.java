package com.example.android.firebaseauth;

import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;
import android.net.ProxyInfo;
import android.net.Uri;
import android.os.AsyncTask;
import android.provider.ContactsContract;
import android.provider.MediaStore;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.bumptech.glide.Glide;
import com.bumptech.glide.RequestBuilder;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthUserCollisionException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.concurrent.ExecutionException;

public class ProfileActivity extends AppCompatActivity implements View.OnClickListener {
    private static final int CHOOSE_IMAGE = 101;
    EditText etName;
    Button bSave;
    Uri uriProfileImage;
    ImageView ivProfilePic;
    Button bVerifyEmail;
    TextView tvIsEmailVerified;
    Toolbar toolbar;

    private StorageReference mStorageRef;
    private FirebaseAuth mAuth;
    FirebaseUser currentUser;
    String profileImageUrl;
    ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        etName = findViewById(R.id.et_Name);
        bSave = findViewById(R.id.b_Save);
        ivProfilePic = findViewById(R.id.iv_profile_pic);
        progressBar = findViewById(R.id.progressBarProfile);
        bVerifyEmail = findViewById(R.id.b_verify_email);
        tvIsEmailVerified = findViewById(R.id.tv_isEmailVerified);
        toolbar = findViewById(R.id.profile_toolbar);
        //setSupportActionBar(toolbar);

        ivProfilePic.setOnClickListener(this);
        bSave.setOnClickListener(this);
        bVerifyEmail.setOnClickListener(this);

        mStorageRef = FirebaseStorage.getInstance().getReference();
        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        loaduserInformation();
    }

    @Override
    public void onStart() {
        super.onStart();

        if (currentUser == null) {
            finish();
            startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        } else {
            profileImageUrl = currentUser.getPhotoUrl().toString();
        }
        checkEmailVerification();


    }

    private void loaduserInformation() {
        if (currentUser != null) {
            if (currentUser.getDisplayName() != null) {
                etName.setText(currentUser.getDisplayName());
            }
            File curUserImgFile = new File(getFilesDir(), currentUser.getEmail()+"");

            //Bitmap curUserImg = MediaStore.Images.Media.getBitmap(getContentResolver(), Uri.fromFile(curUserImgFile));
            if(curUserImgFile.exists()) {
               Glide.with(this)
                        .load(curUserImgFile)
                        .into(ivProfilePic);
                Toast.makeText(ProfileActivity.this, "File image loaded", Toast.LENGTH_SHORT).show();
            }
            Uri photoUrl = currentUser.getPhotoUrl();
            if (photoUrl != null) {
                /*Glide.with(this)
                        .load(currentUser.getPhotoUrl())
                        .into(ivProfilePic);*/
                /*try {
                    //Bitmap image = BitmapFactory.decodeStream(new URL(currentUser.getPhotoUrl().toString()).openConnection().getInputStream());
                    Bitmap image = new ImageDownloadAsync().execute(new URL(photoUrl.toString())).get();
                    ivProfilePic.setImageBitmap(image);
                    image.compress(Bitmap.CompressFormat.JPEG,100, new FileOutputStream(curUserImgFile));
                } catch (IOException e) {
                    e.printStackTrace();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (ExecutionException e) {
                    e.printStackTrace();
                }*/

            }
        }
    }


    private void checkEmailVerification() {
        if (currentUser.isEmailVerified()) {
            tvIsEmailVerified.setText("Email is verified");
            bVerifyEmail.setEnabled(false);
        } else {
            tvIsEmailVerified.setText("Email is not verified. Click above button");
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.logout_menu:
                FirebaseAuth.getInstance().signOut();
                finish();
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
        }
        return true;
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.iv_profile_pic:
                showImageChooser();
                break;
            case R.id.b_Save:
                updateUserProfile();
                break;
            case R.id.b_verify_email:
                Toast.makeText(ProfileActivity.this, "Button clicked", Toast.LENGTH_SHORT).show();
                verifyEmail();
                mAuth.getCurrentUser().reload().addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        checkEmailVerification();
                        Toast.makeText(ProfileActivity.this, "Email verified", Toast.LENGTH_SHORT).show();
                    }
                });
                break;
        }
    }

    private void verifyEmail() {
        currentUser.sendEmailVerification().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                Toast.makeText(ProfileActivity.this, "Verification mail sent", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateUserProfile() {

        String displayName = etName.getText().toString();
        if (displayName == "") {
            etName.setError("Enter a name");
            etName.requestFocus();
            return;
        } else {
            if (currentUser != null) {
                UserProfileChangeRequest profile = new UserProfileChangeRequest.Builder()
                        .setDisplayName(displayName)
                        .setPhotoUri(Uri.parse(profileImageUrl))
                        .build();
                currentUser.updateProfile(profile).addOnCompleteListener(new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        Toast.makeText(ProfileActivity.this, "Profile updated", Toast.LENGTH_SHORT).show();
                    }
                });

            }
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CHOOSE_IMAGE && data != null && resultCode == RESULT_OK) {
            uriProfileImage = data.getData();
            String[] filePathColumn = {MediaStore.Images.Media.DATA};
            Cursor cursor = getContentResolver().query(uriProfileImage,
                    filePathColumn, null, null, null);
            cursor.moveToFirst();
            int columnIndex = cursor.getColumnIndex(filePathColumn[0]);
            String picturePath = cursor.getString(columnIndex);

            Bitmap image = null;
            try {
                image = MediaStore.Images.Media.getBitmap(getContentResolver(), uriProfileImage);
                ivProfilePic.setImageBitmap(image);
                saveImage(image);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    private void saveImage(Bitmap image) {
        File file = new File(getFilesDir(), currentUser.getEmail() + "");
        if(file.exists()){
            Toast.makeText(this, "File already exists,overwriting", Toast.LENGTH_SHORT).show();
        }
        //FileOutputStream fileOutputStream = new FileOutputStream(file);
        try {
            image.compress(Bitmap.CompressFormat.JPEG, 100, new FileOutputStream(file));
            Toast.makeText(this, "image saved", Toast.LENGTH_SHORT).show();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
        uploadImageToFirebase();
    }

    private void uploadImageToFirebase() {
        progressBar.setVisibility(View.VISIBLE);
        long picName = System.currentTimeMillis();
        final StorageReference profilePicReference = mStorageRef.child("profilepics/" + picName + ".jpeg");
        //if(currentUser!=null){
        //  if(uriProfileImage!=null) {
        profilePicReference.putFile(uriProfileImage).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                profilePicReference.getDownloadUrl()
                        .addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {
                                profileImageUrl = uri.toString();
                                Toast.makeText(ProfileActivity.this, "Link got", Toast.LENGTH_SHORT).show();
                                //progressBar.setVisibility(View.GONE);
                            }
                        });
                Toast.makeText(ProfileActivity.this, "Profilepic uploaded", Toast.LENGTH_SHORT).show();
                progressBar.setVisibility(View.GONE);
            }
        });
               /* profilePicReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                    @Override
                    public void onSuccess(Uri uri) {
                        profileImageUrl = uri.toString();
                        Toast.makeText(ProfileActivity.this, "Link got", Toast.LENGTH_SHORT).show();
                    }
                });*/
        //}
        //}
        //progressBar.setVisibility(View.GONE);
    }

    private void showImageChooser() {
        Intent intent = new Intent();
        intent.setType("image/*");
        intent.setAction(Intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent, "Select profile pic"), CHOOSE_IMAGE);
    }

    class ImageDownloadAsync extends AsyncTask<URL, Void,Bitmap>{

        @Override
        protected void onPostExecute(Bitmap bitmap) {
            super.onPostExecute(bitmap);
            Toast.makeText(ProfileActivity.this, "Downloaded profile photo", Toast.LENGTH_SHORT).show();
        }

        @Override
        protected Bitmap doInBackground(URL... urls) {
            URL imageUrl = urls[0];
            Bitmap image = null;
            try {
                image = BitmapFactory.decodeStream(new URL(currentUser.getPhotoUrl().toString()).openConnection().getInputStream());
            } catch (IOException e) {
                e.printStackTrace();
            }
            return image;
        }
    }
}
