package bucci.dev.freestyle;

import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.provider.MediaStore;
import android.util.Log;

/**
 * Created by bucci on 13.06.14.
 */
public class Utils {
    private static final String TAG = "BCC|Utils";

    //MediaPlayer.setDataSource with Uri from ACTION_GET_CONTENT isn't working properly
    // it's workaround using real path
    public static String getImagePathFromUri(Context context, Uri uri) {
        Log.d(TAG, "getImagePathFromUri(), Uri: " + uri.toString());
        Cursor cursor = context.getContentResolver().query(uri, null, null, null, null);
        cursor.moveToFirst();
        String document_id = cursor.getString(0);
        document_id = document_id.substring(document_id.lastIndexOf(":") + 1);
        cursor.close();

        cursor = context.getContentResolver().query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                null, MediaStore.Audio.Media._ID + " = ? ", new String[]{document_id}, null);
        cursor.moveToFirst();
        String path = cursor.getString(cursor.getColumnIndex(MediaStore.Images.Media.DATA));
        cursor.close();

        return path;
    }
}
