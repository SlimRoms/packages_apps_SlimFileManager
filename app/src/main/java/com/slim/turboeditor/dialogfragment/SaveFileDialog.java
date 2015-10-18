/*
 * Copyright (C) 2014 Vlad Mihalachi
 *
 * This file is part of Turbo Editor.
 *
 * Turbo Editor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Turbo Editor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.slim.turboeditor.dialogfragment;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;

import com.slim.slimfilemanager.R;
import com.slim.turboeditor.activity.MainActivity;
import com.slim.turboeditor.task.SaveFileTask;
import com.slim.turboeditor.views.DialogHelper;

import java.io.File;

@SuppressLint("ValidFragment")
public class SaveFileDialog extends DialogFragment {

    File mFile;
    String mText;
    String mEncoding;
    boolean mOpenNewFileAfter;

    @SuppressLint("ValidFragment")
    public SaveFileDialog(File file, String text, String encoding) {
        mFile = file;
        mText = text;
        mEncoding = encoding;
        mOpenNewFileAfter = false;
    }

    @SuppressLint("ValidFragment")
    public SaveFileDialog(File file, String text, String encoding, boolean openNewFileAfter) {
        mFile = file;
        mText = text;
        mEncoding = encoding;
        mOpenNewFileAfter = openNewFileAfter;
    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {

        View view = new DialogHelper.Builder(getActivity())
                .setIcon(getResources().getDrawable(R.drawable.ic_action_save))
                .setTitle(R.string.salva)
                .setMessage(String.format(getString(R.string.save_changes), mFile.getName()))
                .createCommonView();

        return new AlertDialog.Builder(getActivity())
                .setView(view)
                .setPositiveButton(R.string.salva,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                new SaveFileTask((MainActivity) getActivity(), mFile, mText,
                                        mEncoding, new SaveFileTask.SaveFileInterface() {
                                    @Override
                                    public void fileSaved(Boolean success) {
                                        if (getActivity() != null) {
                                            ((MainActivity) getActivity()).savedAFile(mFile);
                                        }
                                    }
                                }).execute();
                            }
                        }
                )
                .setNeutralButton(android.R.string.cancel, null)
                .setNegativeButton(R.string.no,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                ISaveDialog target = (ISaveDialog) getTargetFragment();
                                if (target == null) {
                                    target = (ISaveDialog) getActivity();
                                }
                                target.userDoesntWantToSave(
                                        mOpenNewFileAfter, null
                                );
                            }
                        }
                )
                .create();
    }

    public interface ISaveDialog {
        void userDoesntWantToSave(boolean openNewFile, File newFile);
    }
}