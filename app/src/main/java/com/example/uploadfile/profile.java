package com.example.uploadfile;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import android.Manifest;
import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.provider.OpenableColumns;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.OnProgressListener;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.HashMap;
import java.util.Map;


public class profile extends AppCompatActivity {
    Button scanbtn;
    public static TextView scantext;
    private FirebaseUser user;

    TextView name , mail;
    Button logout;
    Button selectFile, upload;
    TextView notification;
    Uri pdfUri;
    FirebaseStorage storage;
    FirebaseDatabase database;
    ProgressDialog progressDialog;
    Map<String,String> map = new HashMap<>();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);
        logout = findViewById(R.id.out);


        storage = FirebaseStorage.getInstance();
        database = FirebaseDatabase.getInstance();

        selectFile = findViewById(R.id.selectFile);
        upload = findViewById(R.id.upload);
        notification = findViewById(R.id.notification);

        scantext=(TextView)findViewById(R.id.scantext);
        scanbtn=(Button) findViewById(R.id.scanbtn);

        scanbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                startActivity(new Intent(getApplicationContext(),scannerView.class));
            }
        });

        selectFile.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(ContextCompat.checkSelfPermission(profile.this, Manifest.permission.READ_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED)
                {
                    selectPdf();
                }
                else
                    ActivityCompat.requestPermissions(profile.this, new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 9);

            }
        });
        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(pdfUri!= null)
                    uploadFile(pdfUri);
                else
                    Toast.makeText(profile.this,"select a file", Toast.LENGTH_SHORT).show();
            }
        });


        GoogleSignInAccount signInAccount = GoogleSignIn.getLastSignedInAccount(this);
        if(signInAccount!=null){

        }

        logout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FirebaseAuth.getInstance().signOut();
                Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                startActivity(intent);
            }
        });
    }

    private void uploadFile( Uri pdfUri) {
        progressDialog = new ProgressDialog(this);
        progressDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        progressDialog.setTitle("Uploading File....");
        progressDialog.setProgress(0);
        progressDialog.show();
        user =FirebaseAuth.getInstance().getCurrentUser();
        String userid = user.getEmail();
        String fileName = getFileName(pdfUri,getApplicationContext()).replaceAll(".pdf","")+ "";
        StorageReference storageReference = storage.getReference().child("Uploads").child(userid).child(fileName);
        storageReference.putFile(pdfUri).addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        storageReference.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                            @Override
                            public void onSuccess(Uri uri) {

                                map.put("url",uri.toString());
                                Toast.makeText(profile.this,uri.toString(),Toast.LENGTH_SHORT).show();
                                FirebaseFirestore.getInstance().collection("uploads").document(userid).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                                    @Override
                                    public void onComplete(@NonNull Task<Void> task) {
                                        if(task.isSuccessful())
                                            Toast.makeText(profile.this,"File is uploaded",Toast.LENGTH_SHORT).show();
                                        else
                                            Toast.makeText(profile.this,"failed",Toast.LENGTH_SHORT).show();

                                    }
                                });
                            }
                        });
                        /*String url = String.valueOf(taskSnapshot.getMetadata().getReference().getDownloadUrl());
                        //DatabaseReference reference = database.getReference().child(fileName);
                        //reference.setValue(url);
                        Map<String,String> map = new HashMap<>();
                        map.put(fileName,url);*/

                        /*FirebaseFirestore.getInstance().collection("uploads").document(userid).set(map).addOnCompleteListener(new OnCompleteListener<Void>() {
                            @Override
                            public void onComplete(@NonNull Task<Void> task) {
                                if(task.isSuccessful())
                                    Toast.makeText(profile.this,"File is uploaded",Toast.LENGTH_SHORT).show();
                                else
                                    Toast.makeText(profile.this,"failed",Toast.LENGTH_SHORT).show();

                            }
                        });*/
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Toast.makeText(profile.this,"",Toast.LENGTH_SHORT).show();
            }
        }).addOnProgressListener(new OnProgressListener<UploadTask.TaskSnapshot>() {
            @Override
            public void onProgress(@NonNull UploadTask.TaskSnapshot snapshot) {
                int currentProgress = (int) (100*snapshot.getBytesTransferred()/snapshot.getTotalByteCount());
                progressDialog.setProgress(currentProgress);
            }
        });
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if(requestCode==9 && grantResults[0]==PackageManager.PERMISSION_GRANTED)
        {
            selectPdf();
        }
        else
            Toast.makeText(profile.this,"Please Provide Permission",Toast.LENGTH_SHORT).show();

    }
    private String queryName(ContentResolver resolver, Uri uri) {
        Cursor returnCursor =
                resolver.query(uri, null, null, null, null);
        assert returnCursor != null;
        int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
        returnCursor.moveToFirst();
        String name = returnCursor.getString(nameIndex);
        returnCursor.close();
        return name;
    }

    private void selectPdf(){
        Intent intent = new Intent();
        intent.setType("application/pdf");
        intent.setAction(Intent.ACTION_GET_CONTENT);

        startActivityForResult(intent,86);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 86 && resultCode == RESULT_OK && data != null)
        {
            pdfUri = data.getData();
            notification.setText("File is selected: "+ getFileName(pdfUri,getApplicationContext()));
        }
        else{
            Toast.makeText(profile.this, "Please Select file", Toast.LENGTH_SHORT).show();

        }
    }
    String getFileName(Uri uri, Context context){
        String r = null;
        if(uri.getScheme().equals("content")){
            Cursor c = context.getContentResolver().query(pdfUri,null,null,null,null);
            try{
                if(c!=null && c.moveToFirst()){
                    r= c.getString(c.getColumnIndex(OpenableColumns.DISPLAY_NAME));
                }
            }finally {
                c.close();
            }
            if(r==null){
                r = pdfUri.getPath();
                int cut = r.lastIndexOf('/');
                if (cut!=-1){
                    r = r.substring(cut+1);
                }
            }
        }
        return r;
    }
}