package com.kircherelectronics.compassexplorer.datalogger;

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
public interface DataLoggerInterface
{
    void setHeaders(Iterable<String> headers) throws IllegalStateException;
    void addRow(Iterable<String> values) throws IllegalStateException;
    String writeToFile();
}
