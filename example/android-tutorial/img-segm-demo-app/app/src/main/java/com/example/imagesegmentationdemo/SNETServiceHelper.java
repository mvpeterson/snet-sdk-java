package com.example.imagesegmentationdemo;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Log;

import java.io.File;
import java.util.concurrent.TimeUnit;

import io.grpc.ManagedChannel;
import io.grpc.Status;
import io.grpc.StatusRuntimeException;

public class SNETServiceHelper
{
    private HandlerMainActivity handlerMain;
    private ManagedChannel channel = null;

    private Thread threadServiceInit;
    private Thread threadServiceCalling;

    public SNETServiceHelper(HandlerMainActivity handlerMain)
    {
        this.handlerMain = handlerMain;
    }

    public void openProxyServiceChannelAsync()
    {
        threadServiceInit = new Thread(new Runnable()
        {
            public void run()
            {
                handlerMain.sendEmptyMessage(HandlerMainActivity.MSG_DISABLE_ACTIVITY_GUI);
                try
                {
                    //Put it here to be compilable
                    throw new InterruptedException();
                }
                catch (InterruptedException e)
                {
                    Log.e("CloseServiceChannelTask", "Closing channel error", e);
                }
                catch (Exception e)
                {
                    showGeneralError(e);
                }
                handlerMain.sendEmptyMessage(HandlerMainActivity.MSG_ENABLE_ACTIVITY_GUI);
            }
        });

        threadServiceInit.start();
    }

    public void closeServiceChannel()
    {
        interruptServiceInit();
        interruptServiceCall();
        new Thread(new Runnable()
        {
            public void run()
            {
                if (channel != null)
                {
                    try
                    {
                        channel.shutdownNow().awaitTermination(5, TimeUnit.SECONDS);
                    }
                    catch (InterruptedException e)
                    {
                        Log.e("CloseServiceChannelTask", "Closing channel error", e);
                    }
                }
            }
        }).start();
    }

    public void callImageSegmentationServiceAsync(Uri inputUri, int maxImageHeight, int maxImageWidth)
    {
        threadServiceCalling = new Thread(new Runnable()
        {
            public void run()
            {
                handlerMain.sendEmptyMessage(HandlerMainActivity.MSG_DISABLE_ACTIVITY_GUI);

                /*
                *    PREPARE SERVICE INPUT DATA
                */
                byte[] data = new byte[0];
                Bitmap bitmap;
                try
                {
                    bitmap = ImageUtils.handleSamplingAndRotationBitmap(handlerMain.getContext(),
                            inputUri, maxImageWidth, maxImageHeight);
                }
                catch(Exception e)
                {

                }

                /*
                *   CALL SINGULARITYNET SERVICE
                * */
                byte[] bytesInput = data;


                /*
                *   DECODE SERVICE RESPONSE
                 */
                Bitmap decodedBitmap = null;
                handlerMain.handleMessage(handlerMain.obtainMessage(HandlerMainActivity.MSG_SET_IMAGE_BITMAP, decodedBitmap));
                handlerMain.sendEmptyMessage(HandlerMainActivity.MSG_ENABLE_ACTIVITY_GUI);

            }
        });

        threadServiceCalling.start();

    }

    private void showGeneralError(Exception e)
    {
        handlerMain.sendMessage(handlerMain.obtainMessage(
                HandlerMainActivity.MSG_SHOW_ERROR, e.getMessage()));
    }

    private void interruptServiceCall()
    {
        if(null != threadServiceCalling)
        {
            threadServiceCalling.interrupt();
        }
    }

    private void interruptServiceInit()
    {
        if(null != threadServiceInit)
        {
            threadServiceInit.interrupt();
        }
    }

}
