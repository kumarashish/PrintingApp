package com.zebra.signaturecapturedemo;

import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.zebra.sdk.comm.BluetoothConnection;
import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.comm.TcpConnection;
import com.zebra.sdk.graphics.internal.ZebraImageAndroid;
import com.zebra.sdk.printer.PrinterLanguage;
import com.zebra.sdk.printer.PrinterStatus;
import com.zebra.sdk.printer.SGD;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinterLanguageUnknownException;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

public class MainActivity extends ConnectionScreen {

    private ProgressDialog myDialog;
    private UIHelper helper = new UIHelper(this);
    private Connection connection;
    private SignatureArea signatureArea;
    private AlertDialog.Builder builder;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

    }

    /**
     * This is an abstract method from connection screen where we are implementing the calls.
     */
    @Override
    public void performTest() {
        new Thread(new Runnable() {
            public void run() {
                Looper.prepare();
                doPerformTest();
                Looper.loop();
                Looper.myLooper().quit();
            }
        }).start();

    }

    /**
     * This method is used to create a new alert dialog to sign and print and implements best practices to check the status of the printer.
     */
    public void doPerformTest() {
        if (isBluetoothSelected() == false) {
            try {
                int port = Integer.parseInt(getTcpPortNumber());
                connection = new TcpConnection(getTcpAddress(), port);
            } catch (NumberFormatException e) {
                helper.showErrorDialogOnGuiThread("Port number is invalidd");
                return;
            }
        } else {
            connection = new BluetoothConnection(getMacAddressFieldText());
        }
        try {
            connection.open();
            final ZebraPrinter printer = ZebraPrinterFactory.getInstance(connection);
            ZebraPrinterLinkOs linkOsPrinter = ZebraPrinterFactory.createLinkOsPrinter(printer);
            PrinterStatus printerStatus = (linkOsPrinter != null) ? linkOsPrinter.getCurrentStatus() : printer.getCurrentStatus();
            getPrinterStatus();
            if (printerStatus.isReadyToPrint) {
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {

                        Toast.makeText(MainActivity.this, "Printer Ready", Toast.LENGTH_LONG).show();

                    }
                });

                        View view = View.inflate(MainActivity.this, R.layout.signature_print_dialog, null);
                        signatureArea = (SignatureArea) view.findViewById(R.id.signatureArea);
                        builder = new AlertDialog.Builder(MainActivity.this, AlertDialog.THEME_DEVICE_DEFAULT_LIGHT);
                        builder.setView(view);
                        builder.setPositiveButton(getString(R.string.print), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {

                                try {
                                    connection.open();
                                    sendTestLabel();

//                                    Bitmap icon = BitmapFactory.decodeResource(getResources(),
//                                           R.drawable.brogas_logo);
////                                    Bitmap signatureBitmap = Bitmap.createScaledBitmap(signatureArea.getBitmap(), 300, 200, false);
//
//                                   // printer.printImage(new ZebraImageAndroid(signatureBitmap), 0, 0, signatureBitmap.getWidth(), signatureBitmap.getHeight(), false);
//                                            Bitmap logo = Bitmap.createScaledBitmap(icon, 400, 300, false);
//                                            printer.printImage(new ZebraImageAndroid(logo), 0, 0, logo.getWidth(), logo.getHeight(), false);
                                        dialog.dismiss();

                                  //  printer.sendFileContents("^FT78,76^A0N,28,28^FH_^FDHello_0AWorld^FS");

                                    connection.close();

                                } catch (ConnectionException e) {
                                    helper.showErrorDialogOnGuiThread(e.getMessage());
                                }
                            }
                        });
                        builder.setNegativeButton(getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.dismiss();
                            }
                        });
                        builder.create();
                        builder.show();


            } else if (printerStatus.isHeadOpen) {
                helper.showErrorMessage("Error: Head Open \nPlease Close Printer Head to Print");
            } else if (printerStatus.isPaused) {
                helper.showErrorMessage("Error: Printer Paused");
            } else if (printerStatus.isPaperOut) {
                helper.showErrorMessage("Error: Media Out \nPlease Load Media to Print");
            } else {
                helper.showErrorMessage("Error: Please check the Connection of the Printer");
            }

            connection.close();
            getAndSaveSettings();
        } catch (ConnectionException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } catch (ZebraPrinterLanguageUnknownException e) {
            helper.showErrorDialogOnGuiThread(e.getMessage());
        } finally {
            helper.dismissLoadingDialog();
        }

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }


    private void createCancelProgressDialog(final String message) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                myDialog = new ProgressDialog(MainActivity.this, R.style.ErrorButtonAppearance);
                myDialog.setMessage(message);
                myDialog.setButton(DialogInterface.BUTTON_NEGATIVE, "OK", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        // Use either finish() or return() to either close the activity or just the dialog
                        return;
                    }
                });
                myDialog.show();
                TextView tv1 = (TextView) myDialog.findViewById(android.R.id.message);
                tv1.setTextAppearance(MainActivity.this, R.style.ErrorButtonAppearance);
                Button b = myDialog.getButton(DialogInterface.BUTTON_NEGATIVE);
                b.setTextColor(getResources().getColor(R.color.light_gray));
                b.setBackgroundColor(getResources().getColor(R.color.zebra_blue));
            }
        });

    }

    /**
     * This method implements the best practices to check the language of the printer and set the language of the printer to ZPL.
     *
     * @throws ConnectionException
     */
    private void getPrinterStatus() throws ConnectionException{


        final String printerLanguage = SGD.GET("device.languages", connection); //This command is used to get the language of the printer.

        final String displayPrinterLanguage = "Printer Language is " + printerLanguage;

        SGD.SET("device.languages", "zpl", connection); //This command set the language of the printer to ZPL

        MainActivity.this.runOnUiThread(new Runnable() {
            @Override
            public void run() {

                Toast.makeText(MainActivity.this, displayPrinterLanguage + "\n" + "Language set to ZPL", Toast.LENGTH_LONG).show();

            }
        });

    }

    /**
     * This method saves the entered address of the printer.
     */

    private void getAndSaveSettings() {
        SettingsHelper.saveBluetoothAddress(MainActivity.this, getMacAddressFieldText());
        SettingsHelper.saveIp(MainActivity.this, getTcpAddress());
        SettingsHelper.savePort(MainActivity.this, getTcpPortNumber());
    }


    private void sendTestLabel() {
        try {
            byte[] configLabel = createZplReceipt().getBytes();
            connection.write(configLabel);

        } catch (ConnectionException e) {
        }
    }

    private String createZplReceipt() {
        /*
         This routine is provided to you as an example of how to create a variable length label with user specified data.
         The basic flow of the example is as follows
            Header of the label with some variable data
            Body of the label
                Loops thru user content and creates small line items of printed material
            Footer of the label

         As you can see, there are some variables that the user provides in the header, body and footer, and this routine uses that to build up a proper ZPL string for printing.
         Using this same concept, you can create one label for your receipt header, one for the body and one for the footer. The body receipt will be duplicated as many items as there are in your variable data

         */

        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com/content/dam/zebra/manuals/en-us/printer/zplii-pm-vol2-en.pdf

         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing.
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" +

                        "^PON^PW400^MNN^LL%d^LH0,0" + "\r\n" +

                        "^FO50,50" + "\r\n" + "^A0,N,50,50" + "\r\n" + "^FD Brothers Gas^FS" + "\r\n" +

                        "^FO50,130" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FDPayment Receipt^FS" + "\r\n" +
                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS"+

                        "^FO50,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDReceipt No:^FS" + "\r\n" +

                        "^FO225,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDRMRC-U1L1900113^FS" + "\r\n" +

                        "^FO50,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDDate:^FS" + "\r\n" +

                        "^FO225,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" +

                        "^FO50,273" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDAddress^FS" + "\r\n" +

                        "^FO225,273" + "\r\n" + "^A0,N,15,15" + "\r\n" + "^FDTema Business Solutions,Madhapur^FS" + "\r\n" +
                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS";

        int headerHeight = 325;
        String body = String.format("^LH0,%d", headerHeight);

        int heightOfOneLine = 40;

        float totalPrice = 0;

       // Map<String, String> itemsToPrint = createListOfItems();

        int i = 0;
//        for (String productName : itemsToPrint.keySet()) {
//            String price = itemsToPrint.get(productName);
//
//            String lineItem = "^FO50,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO280,%d" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD$%s^FS";
//            totalPrice += Float.parseFloat(price);
//            int totalHeight = i++ * heightOfOneLine;
//            body += String.format(lineItem, totalHeight, productName, totalHeight, price);
//
//        }

       // long totalBodyHeight = (itemsToPrint.size() + 1) * heightOfOneLine;
        long totalBodyHeight =0;

        long footerStartPosition = headerHeight;

        String footer = String.format("^LH0,%d" + "\r\n" +

                "^FO50,1" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDTotal^FS" + "\r\n" +

                "^FO175,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDAED 200^FS" + "\r\n" +

                "^FO50,130" + "\r\n" + "^A0,N,45,45" + "\r\n" + "^FDSignature^FS" + "\r\n" +

                "^FO50,190" + "\r\n" + "^GB350,200,2,B^FS" + "\r\n" +

                "^FO50,400" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,420" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDThanks for choosing Brothers Gas us!^FS" + "\r\n" +

                "^FO50,470" + "\r\n" + "^B3N,N,45,Y,N" + "\r\n" + "^FD0123456^FS" + "\r\n" + "^XZ", footerStartPosition, totalPrice);

        long footerHeight = 600;
        long labelLength = headerHeight + totalBodyHeight + footerHeight;

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = sdf.format(date);

        String header = String.format(tmpHeader, labelLength, dateString);

        String wholeZplLabel = String.format("%s%s%s", header, body, footer);

        return wholeZplLabel;
    }


    private void sendZplReceipt(Connection printerConnection) throws ConnectionException {
        /*
         This routine is provided to you as an example of how to create a variable length label with user specified data.
         The basic flow of the example is as follows
            Header of the label with some variable data
            Body of the label
                Loops thru user content and creates small line items of printed material
            Footer of the label

         As you can see, there are some variables that the user provides in the header, body and footer, and this routine uses that to build up a proper ZPL string for printing.
         Using this same concept, you can create one label for your receipt header, one for the body and one for the footer. The body receipt will be duplicated as many items as there are in your variable data

         */

        String tmpHeader =
        /*
         Some basics of ZPL. Find more information here : http://www.zebra.com

         ^XA indicates the beginning of a label
         ^PW sets the width of the label (in dots)
         ^MNN sets the printer in continuous mode (variable length receipts only make sense with variably sized labels)
         ^LL sets the length of the label (we calculate this value at the end of the routine)
         ^LH sets the reference axis for printing.
            You will notice we change this positioning of the 'Y' axis (length) as we build up the label. Once the positioning is changed, all new fields drawn on the label are rendered as if '0' is the new home position
         ^FO sets the origin of the field relative to Label Home ^LH
         ^A sets font information
         ^FD is a field description
         ^GB is graphic boxes (or lines)
         ^B sets barcode information
         ^XZ indicates the end of a label
         */

                "^XA" +

                        "^POI^PW400^MNN^LL325^LH0,0" + "\r\n" +

                        "^FO50,50" + "\r\n" + "^A0,N,70,70" + "\r\n" + "^FD Shipping^FS" + "\r\n" +

                        "^FO50,130" + "\r\n" + "^A0,N,35,35" + "\r\n" + "^FDPurchase Confirmation^FS" + "\r\n" +

                        "^FO50,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDCustomer:^FS" + "\r\n" +

                        "^FO225,180" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDAcme Industries^FS" + "\r\n" +

                        "^FO50,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDDelivery Date:^FS" + "\r\n" +

                        "^FO225,220" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FD%s^FS" + "\r\n" +

                        "^FO50,273" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDItem^FS" + "\r\n" +

                        "^FO280,273" + "\r\n" + "^A0,N,25,25" + "\r\n" + "^FDPrice^FS" + "\r\n" +

                        "^FO50,300" + "\r\n" + "^GB350,5,5,B,0^FS" + "^XZ";

        int headerHeight = 325;

        Date date = new Date();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy/MM/dd");
        String dateString = sdf.format(date);

        String header = String.format(tmpHeader, dateString);

        printerConnection.write(header.getBytes());

        int heightOfOneLine = 40;

        float totalPrice = 0;

        Map<String, String> itemsToPrint = createListOfItems();

        int i = 0;
        for (String productName : itemsToPrint.keySet()) {
            String price = itemsToPrint.get(productName);

            String lineItem = "^XA^POI^LL40" + "^FO50,10" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD%s^FS" + "\r\n" + "^FO280,10" + "\r\n" + "^A0,N,28,28" + "\r\n" + "^FD$%s^FS" + "^XZ";
            totalPrice += Float.parseFloat(price);
            String oneLineLabel = String.format(lineItem, productName, price);

            printerConnection.write(oneLineLabel.getBytes());

        }

        long totalBodyHeight = (itemsToPrint.size() + 1) * heightOfOneLine;

        long footerStartPosition = headerHeight + totalBodyHeight;

        String footer = String.format("^XA^POI^LL600" + "\r\n" +

                "^FO50,1" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FDTotal^FS" + "\r\n" +

                "^FO175,15" + "\r\n" + "^A0,N,40,40" + "\r\n" + "^FD$%.2f^FS" + "\r\n" +

                "^FO50,130" + "\r\n" + "^A0,N,45,45" + "\r\n" + "^FDPlease Sign Below^FS" + "\r\n" +

                "^FO50,190" + "\r\n" + "^GB350,200,2,B^FS" + "\r\n" +

                "^FO50,400" + "\r\n" + "^GB350,5,5,B,0^FS" + "\r\n" +

                "^FO50,420" + "\r\n" + "^A0,N,30,30" + "\r\n" + "^FDThanks for choosing us!^FS" + "\r\n" +

                "^FO50,470" + "\r\n" + "^B3N,N,45,Y,N" + "\r\n" + "^FD0123456^FS" + "\r\n" + "^XZ", totalPrice);

        printerConnection.write(footer.getBytes());

    }
    private Map<String, String> createListOfItems() {
        String[] names = { "Microwave Oven", "Sneakers (Size 7)", "XL T-Shirt", "Socks (3-pack)", "Blender", "DVD Movie" };
        String[] prices = { "79.99", "69.99", "39.99", "12.99", "34.99", "16.99" };
        Map<String, String> retVal = new HashMap<String, String>();

        for (int ix = 0; ix < names.length; ix++) {
            retVal.put(names[ix], prices[ix]);
        }
        return retVal;
    }



}
