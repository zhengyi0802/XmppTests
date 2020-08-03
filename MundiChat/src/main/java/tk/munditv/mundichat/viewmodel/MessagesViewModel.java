package tk.munditv.mundichat.viewmodel;

import android.content.Context;
import android.os.RemoteException;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

import tk.munditv.mundichat.MyApplication;
import tk.munditv.mundichat.callback.MessageListener;
import tk.munditv.mundichat.database.SqLiteDatabase;
import tk.munditv.mundichat.database.models.Message;
import tk.munditv.mundichat.database.providers.MessagesProvider;
import tk.munditv.mundichat.utils.Logger;

public class MessagesViewModel extends ViewModel
        implements MessageListener {

    private static final String TAG = "MessagesViewModel";
    private static String mDatabaseName;
    private SqLiteDatabase mDatabase;
    private MessagesProvider messagesProvider;
    private String account;
    private String recipient;

    private ArrayList<Message> messages;
    private MutableLiveData<ArrayList<Message>> mMessages;

    public MessagesViewModel() {
        Logger.debug(TAG, "MessagesViewModel()");
        mMessages = new MutableLiveData<>();
    }

    public void initialize(Context mContext, String recipient) throws RemoteException {
        Logger.debug(TAG, "initialize()");
        mDatabaseName = MyApplication.getInstance().doGetDatabaseName();
        if (mDatabaseName != null) {
            mDatabase = new SqLiteDatabase(mContext, mDatabaseName);
            messagesProvider = new MessagesProvider(mDatabase);
        }
        account = MyApplication.getInstance().getUsername();
        messages = new ArrayList<Message>();
        this.recipient = recipient;
        refreshMessage(recipient);
    }

    public LiveData<ArrayList<Message>> getMessages() {
        return mMessages;
    }

    public void refreshMessage(String remoteAccount) {
        Logger.debug(TAG, "refreshMessage(\""
                + remoteAccount + ") recipient = " + recipient);
        if (!remoteAccount.contains(recipient)) return;
        List<Message> arrayList = new ArrayList<Message>();
        messages = new ArrayList<Message>();
        arrayList = messagesProvider.getMessagesWithRecipient(account, recipient);
        for (Message m : arrayList) {
            String message = m.getMessage();
            long timestamp = m.getCreationTimestamp();
            String date = getDate(timestamp);
            messages.add(m);
        }
        mMessages.postValue(messages);
    }

    @Override
    public void onMessageAdded(String remoteAccount, boolean incoming) {
        Logger.debug(TAG, "onMessageAdded(\""
                + remoteAccount +"\", " + incoming + ")");
        refreshMessage(remoteAccount);
        return;
    }

    @Override
    public void onConversationsCleared(String remoteAccount) {
        Logger.debug(TAG, "onConversationsCleared(\""
                + remoteAccount + "\")");
    }

    @Override
    public void onConversationsClearError(String remoteAccount) {
        Logger.debug(TAG, "onConversationsClearError(\""
                + remoteAccount + "\")");
    }

    @Override
    public void onMessageSent(long messageId) {
        Logger.debug(TAG, "onMessageSent(" + messageId + ")");
    }

    @Override
    public void onMessageDeleted(long messageId) {
        Logger.debug(TAG, "onMessageDeleted(" + messageId + ")");
    }

    private String getDate(long milliSeconds) {
        // Create a DateFormatter object for displaying date in specified
        // format.
        SimpleDateFormat formatter = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
        // Create a calendar object that will convert the date and time value in
        // milliseconds to date.
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(milliSeconds);
        return formatter.format(calendar.getTime());
    }
}
