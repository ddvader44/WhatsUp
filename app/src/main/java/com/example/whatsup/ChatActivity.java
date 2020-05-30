package com.example.whatsup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import com.example.whatsup.Chat.ChatObject;
import com.example.whatsup.Chat.MediaAdapter;
import com.example.whatsup.Chat.MessageAdapter;
import com.example.whatsup.Chat.MessageObject;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class ChatActivity extends AppCompatActivity {
    private RecyclerView mChat,mMedia;
    private RecyclerView.Adapter mChatAdapter,mMediaAdapter;
    private RecyclerView.LayoutManager mChatLayoutManager,mMediaLayoutManager;
    ArrayList<MessageObject> messageList;
    String chatID;
    DatabaseReference mChatDb;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);
        chatID = getIntent().getExtras().getString("chatID");
        mChatDb = FirebaseDatabase.getInstance().getReference().child("chat").child(chatID);
        Button mSend = findViewById(R.id.send);
        Button mAddMedia = findViewById(R.id.addMedia);
        mSend.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { sendMessage(); }});
        mAddMedia.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { openGallery(); }});
        initializeMessageRecyclerView();
        initializeMedia();
        getChatMessages();
    }
    int PICK_INTENT = 1;
    ArrayList<String> mediaUriList = new ArrayList<>();
    private void openGallery() {

        Intent intent = new Intent();
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE,true);
        intent.setAction(intent.ACTION_GET_CONTENT);
        startActivityForResult(Intent.createChooser(intent,"Select Picture(s)"),PICK_INTENT);

    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(resultCode == RESULT_OK)
        {
            if(requestCode == PICK_INTENT)
            {
                if(data.getClipData()==null) {
                    mediaUriList.add(data.getData().toString());
                }else
                {
                    for(int i=0;i<data.getClipData().getItemCount();i++)
                    {
                        mediaUriList.add(data.getClipData().getItemAt(i).getUri().toString());
                    }
                }
                mMediaAdapter.notifyDataSetChanged();
            }
        }
    }

    private void getChatMessages() {
        mChatDb.addChildEventListener(new ChildEventListener() {
            @Override
            public void onChildAdded(@NonNull DataSnapshot dataSnapshot, @Nullable String s) {
                if(dataSnapshot.exists())
                {
                    String text ="",creatorId="";
                    ArrayList<String> mediaUrllist = new ArrayList<>();
                    if(dataSnapshot.child("text").getValue() != null)
                    {
                        text = dataSnapshot.child("text").getValue().toString();
                    }
                    if(dataSnapshot.child("creator").getValue() != null)
                    {
                        creatorId = dataSnapshot.child("creator").getValue().toString();
                    }
                    if(dataSnapshot.child("media").getChildrenCount() >0)
                    {
                        for(DataSnapshot mediaSnapshot : dataSnapshot.child("media").getChildren())
                        {
                            mediaUrllist.add(mediaSnapshot.getValue().toString());
                        }
                    }
                    MessageObject mMessage = new MessageObject(dataSnapshot.getKey(),creatorId,text,mediaUrllist);
                    messageList.add(mMessage);
                    mChatLayoutManager.scrollToPosition(messageList.size()-1);
                    mChatAdapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onChildChanged(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onChildRemoved(@NonNull DataSnapshot dataSnapshot) { }
            @Override
            public void onChildMoved(@NonNull DataSnapshot dataSnapshot, @Nullable String s) { }
            @Override
            public void onCancelled(@NonNull DatabaseError databaseError) { }
        });
    }
    ArrayList<String> mediaIdList = new ArrayList<>();
    int totalMediaUploaded = 0;
    EditText mMessage;
    private void sendMessage() {
        mMessage = findViewById(R.id.message);
            String messageId = mChatDb.push().getKey();
            final DatabaseReference newMessageDb = mChatDb.child(messageId);
           final  Map newMessageMap = new HashMap<>();

           newMessageMap.put("creator", FirebaseAuth.getInstance().getUid());
            if(!mMessage.getText().toString().isEmpty()) {
                newMessageMap.put("text", mMessage.getText().toString());
            }
            if(!mediaUriList.isEmpty())
            {
                for(final String mediaUri : mediaUriList)
                {
                    String mediaId = newMessageDb.child("media").push().getKey();
                    mediaIdList.add(mediaId);
                    final StorageReference filePath = FirebaseStorage.getInstance().getReference().child("chat").child(chatID).child(messageId).child(mediaId);
                    UploadTask uploadTask = filePath.putFile(Uri.parse(mediaUri));
                    uploadTask.addOnSuccessListener(new OnSuccessListener<UploadTask.TaskSnapshot>() {
                        @Override
                        public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                            filePath.getDownloadUrl().addOnSuccessListener(new OnSuccessListener<Uri>() {
                                @Override
                                public void onSuccess(Uri uri) {
                                    newMessageMap.put("/media/" + mediaIdList.get(totalMediaUploaded)+"/",uri.toString());
                                    totalMediaUploaded++;
                                    if(totalMediaUploaded == mediaUriList.size())
                                    {
                                        updateDatabaseWithNewMessage(newMessageDb,newMessageMap);
                                    }
                                }
                            });
                        }
                    });
                }

            }
            else
            {
                if(!mMessage.getText().toString().isEmpty()){
                    updateDatabaseWithNewMessage(newMessageDb,newMessageMap);
                }
            }


        mMessage.setText(null);
    }
    private void updateDatabaseWithNewMessage(DatabaseReference newMessageDb,Map newMessageMap){
    newMessageDb.updateChildren(newMessageMap);
    mMessage.setText(null);
    mediaUriList.clear();
    mediaIdList.clear();
    mMediaAdapter.notifyDataSetChanged();
    }
    private void initializeMessageRecyclerView() {
        messageList = new ArrayList<>();
        mChat = findViewById(R.id.recyclerView);
        mChat.setNestedScrollingEnabled(false);
        mChat.setHasFixedSize(false);
        mChatLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.VERTICAL,false);
        mChat.setLayoutManager(mChatLayoutManager);
        mChatAdapter = new MessageAdapter(messageList);
        mChat.setAdapter(mChatAdapter);
    }
    private void initializeMedia() {
       mediaUriList = new ArrayList<>();
        mMedia = findViewById(R.id.mediaList);
        mMedia.setNestedScrollingEnabled(false);
        mMedia.setHasFixedSize(false);
        mMediaLayoutManager = new LinearLayoutManager(getApplicationContext(), LinearLayout.HORIZONTAL,false);
        mMedia.setLayoutManager(mChatLayoutManager);
        mMediaAdapter = new MediaAdapter(getApplicationContext(),mediaUriList);
        mMedia.setAdapter(mMediaAdapter);
    }
}
