package com.example.solulab6ciclopasado;

import android.Manifest;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.content.pm.PackageManager;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;

public class Notificaciones {
    private static final String CANAL = "pronosticos";

    public static void preparar(AppCompatActivity activity) {
        NotificationManager manager = activity.getSystemService(NotificationManager.class);
        manager.createNotificationChannel(new NotificationChannel(
                CANAL, "Registro y edición de pronósticos", NotificationManager.IMPORTANCE_DEFAULT));
        // minSdk es 34: todos los dispositivos admitidos necesitan este permiso.
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(activity,
                    new String[]{Manifest.permission.POST_NOTIFICATIONS}, 100);
        }
    }

    public static void mostrar(AppCompatActivity activity, String mensaje) {
        if (ActivityCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                != PackageManager.PERMISSION_GRANTED) return;
        Intent intent = new Intent(activity, ListaPronosticosActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent destino = PendingIntent.getActivity(activity, 0, intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        NotificationCompat.Builder aviso = new NotificationCompat.Builder(activity, CANAL)
                .setSmallIcon(R.drawable.ic_balon)
                .setContentTitle("Mis pronósticos")
                .setContentText(mensaje)
                .setContentIntent(destino)
                .setAutoCancel(true);
        NotificationManagerCompat.from(activity).notify(10, aviso.build());
    }
}
