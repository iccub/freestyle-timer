package bucci.dev.freestyle;

import android.annotation.TargetApi;
import android.content.ContentUris;
import android.content.Context;
import android.database.Cursor;
import android.media.MediaMetadataRetriever;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.util.Log;

class MediaUtils {
    private static final String TAG = "BCC|MediaUtils";
    private static final String SONG_PATH_EMPTY_VALUE = "";

    public static boolean isSongLongEnough(String songPath, TimerType timerType) {
        MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
        songRetriever.setDataSource(songPath);
        String durationMetadata = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);
        long songDuration = Long.parseLong(durationMetadata);

        switch (timerType) {
            case BATTLE:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + TimerActivity.BATTLE_DURATION);
                return songDuration >= TimerActivity.BATTLE_DURATION;
            case QUALIFICATION:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + TimerActivity.QUALIFICATION_DURATION);
                return songDuration >= TimerActivity.QUALIFICATION_DURATION;
            case ROUTINE:
                Log.d(TAG, "isSongLongEnough(): " + songDuration + " > " + TimerActivity.routine_duration);
                return songDuration >= TimerActivity.routine_duration;
        }

        return false;
    }

    public static String getSongName(String songPath) {
        MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
        songRetriever.setDataSource(songPath);

        String songTitle = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
        String artist = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);


        StringBuilder buf = new StringBuilder();
        buf.append(artist);
        buf.append(" - ");
        buf.append(songTitle);

        //TODO make song title length enough depending on screen size
        if (buf.length() > 32)
            buf.replace(30, 31, "..");

        return buf.toString();
    }

    public static long getSongDuration(String savedSongPath) {
        if (!savedSongPath.equals(SONG_PATH_EMPTY_VALUE)) {
            MediaMetadataRetriever songRetriever = new MediaMetadataRetriever();
            songRetriever.setDataSource(savedSongPath);
            String durationMetadata = songRetriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION);

            return Long.parseLong(durationMetadata);
        }

        return 0;
    }

    /**
     * Paul Burke method from https://github.com/iPaulPro/aFileChooser to get song path
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @author paulburke
     */
    @TargetApi(Build.VERSION_CODES.KITKAT)
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    private static String getDataColumn(Context context, Uri uri, String selection,
                                        String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

}

