/*
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.firebase.ui.database;

import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.concurrent.Callable;

import static com.firebase.ui.database.TestUtils.getAppInstance;
import static com.firebase.ui.database.TestUtils.isValuesEqual;
import static com.firebase.ui.database.TestUtils.runAndWaitUntil;

@RunWith(AndroidJUnit4.class)
public class FirebaseIndexArrayTest {
    private static final int INITIAL_SIZE = 3;

    private DatabaseReference mRef;
    private DatabaseReference mKeyRef;
    private ObservableSnapshotArray<Integer> mArray;
    private ChangeEventListener mListener;

    @Before
    public void setUp() throws Exception {
        FirebaseDatabase databaseInstance =
                FirebaseDatabase.getInstance(getAppInstance(ApplicationProvider.getApplicationContext()));
        mRef = databaseInstance.getReference().child("firebasearray");
        mKeyRef = databaseInstance.getReference().child("firebaseindexarray");

        mArray = new FirebaseIndexArray<>(mKeyRef, mRef, new ClassSnapshotParser<>(Integer.class));
        mRef.removeValue();
        mKeyRef.removeValue();

        mListener = runAndWaitUntil(mArray, () -> {
            for (int i = 1; i <= INITIAL_SIZE; i++) {
                TestUtils.pushValue(mKeyRef, mRef, i, i);
            }
        }, () -> mArray.size() == INITIAL_SIZE);
    }

    @After
    public void tearDown() {
        mArray.removeChangeEventListener(mListener);
        mRef.getRoot().removeValue();
    }

    @Test
    public void testPushIncreasesSize() throws Exception {
        runAndWaitUntil(mArray,
                () -> TestUtils.pushValue(mKeyRef, mRef, 4, null),
                () -> mArray.size() == 4);
    }

    @Test
    public void testPushAppends() throws Exception {
        runAndWaitUntil(mArray,
                () -> TestUtils.pushValue(mKeyRef, mRef, 4, 4),
                () -> mArray.get(3).equals(4));
    }

    @Test
    public void testAddValueWithPriority() throws Exception {
        runAndWaitUntil(mArray,
                () -> TestUtils.pushValue(mKeyRef, mRef, 4, 0.5),
                () -> mArray.get(3).equals(3) && mArray.get(0).equals(4));
    }

    @Test
    public void testChangePriorities() throws Exception {
        runAndWaitUntil(mArray,
                () -> mKeyRef.child(mArray.getSnapshot(2).getKey()).setPriority(0.5),
                () -> isValuesEqual(mArray, new int[]{3, 1, 2}));
    }
}
