package com.kircherelectronics.compassexplorer.datalogger;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/*
 * Copyright 2013-2017, Kaleb Kircher - Kircher Engineering, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


/**
 * Created by KircherEngineerH on 4/27/2016.
 */
public class CsvDataLogger implements DataLoggerInterface
{
    private CSVPrinter csv;
    private FileWriter fileWriter;
    private boolean headersSet;
    private File file;
    private Context context;

    public CsvDataLogger(Context context, File file)
    {
        this.context = context;
        this.file = file;

        CSVFormat csvFileFormat = CSVFormat.DEFAULT.withRecordSeparator(System.getProperty("line.separator"));

        try
        {
            fileWriter = new FileWriter(file);
            csv = new CSVPrinter(fileWriter, csvFileFormat);
        } catch (IOException e)
        {
            e.printStackTrace();
        }

        headersSet = false;
    }

    public void setHeaders(Iterable<String> headers) throws IllegalStateException
    {
        if(!headersSet)
        {
            try
            {
                csv.printRecord(headers);
                headersSet = true;
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            throw new IllegalStateException("Headers already exist!");
        }
    }

    public void addRow(Iterable<String> values) throws IllegalStateException
    {
        if(headersSet)
        {
            try
            {
                csv.printRecord(values);
            } catch (IOException e)
            {
                e.printStackTrace();
            }
        }
        else
        {
            throw new IllegalStateException("Headers do not exist!");
        }
    }

    public String writeToFile()
    {
        try {
            fileWriter.flush();
            fileWriter.close();
            csv.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));

        return file.getPath();
    }
}
