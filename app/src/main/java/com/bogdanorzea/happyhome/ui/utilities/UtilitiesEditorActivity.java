package com.bogdanorzea.happyhome.ui.utilities;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.bogdanorzea.happyhome.R;
import com.bogdanorzea.happyhome.data.Utility;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import timber.log.Timber;

import static com.bogdanorzea.happyhome.utils.FirebaseUtils.HOMES_PATH;
import static com.bogdanorzea.happyhome.utils.FirebaseUtils.UTILITIES_PATH;
import static com.bogdanorzea.happyhome.utils.FirebaseUtils.Utility.deleteUtility;

public class UtilitiesEditorActivity extends AppCompatActivity {

    private static final String UTILITY_KEY = "utility_key";
    private String mUserUid;
    private String mHomeId;
    private String mUtilityId;
    private EditText mCompanyNameEditText;
    private EditText mCompanyWebsiteEditText;
    private EditText mUtilityNameEditText;
    private Utility mUtility;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_utility_editor);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCompanyNameEditText = findViewById(R.id.company_name);
        mCompanyWebsiteEditText = findViewById(R.id.company_website);
        mUtilityNameEditText = findViewById(R.id.utility_name);

        Intent intent = getIntent();
        if (intent != null) {
            if (intent.hasExtra("userUid") && intent.hasExtra("homeId")) {
                mUserUid = intent.getStringExtra("userUid");
                mHomeId = intent.getStringExtra("homeId");
            }

            if (intent.hasExtra("utilityId")) {
                setTitle(getString(R.string.title_edit_utility));
                mUtilityId = intent.getStringExtra("utilityId");
            } else {
                setTitle(getString(R.string.title_add_utility));
            }
        }

        if (savedInstanceState != null) {
            mUtility = savedInstanceState.getParcelable(UTILITY_KEY);
            displayUtility();
        } else if (!TextUtils.isEmpty(mHomeId) && !TextUtils.isEmpty(mUtilityId)) {
            displayFromFirebase(mHomeId, mUtilityId);
        } else {
            mUtility = new Utility();
        }
    }

    private void saveCurrentUtilityToFirebase() {
        String companyNameString = mCompanyNameEditText.getText().toString();
        String companyWebsiteString = mCompanyWebsiteEditText.getText().toString();
        String meterNameString = mUtilityNameEditText.getText().toString();

        if (TextUtils.isEmpty(companyNameString) || TextUtils.isEmpty(companyWebsiteString) ||
                TextUtils.isEmpty(meterNameString)) {
            Toast.makeText(UtilitiesEditorActivity.this, R.string.toast_data_is_missing, Toast.LENGTH_SHORT).show();
            return;
        }

        mUtility.home_id = mHomeId;
        mUtility.name = meterNameString;
        mUtility.company_name = companyNameString;
        mUtility.company_website = companyWebsiteString;

        DatabaseReference databaseReference = null;
        if (!TextUtils.isEmpty(mUtilityId)) {
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference()
                    .child(UTILITIES_PATH)
                    .child(mUtilityId);
        } else {
            databaseReference = FirebaseDatabase.getInstance()
                    .getReference()
                    .child(UTILITIES_PATH)
                    .push();

            FirebaseDatabase.getInstance().getReference()
                    .child(HOMES_PATH)
                    .child(mHomeId)
                    .child(UTILITIES_PATH)
                    .child(databaseReference.getKey())
                    .setValue(true);
        }

        databaseReference.child("home_id").setValue(mUtility.home_id);
        databaseReference.child("name").setValue(mUtility.name);
        databaseReference.child("company_name").setValue(mUtility.company_name);
        databaseReference.child("company_website").setValue(mUtility.company_website);

        finish();
    }

    private void displayFromFirebase(final String mHomeId, final String utilityId) {
        FirebaseDatabase.getInstance()
                .getReference()
                .child(UTILITIES_PATH)
                .child(utilityId)
                .addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot dataSnapshot) {
                        mUtility = dataSnapshot.getValue(Utility.class);
                        if (mUtility != null) {
                            displayUtility();
                        } else {
                            Timber.d("Error retrieving utility with id %s", utilityId);
                        }
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError databaseError) {

                    }
                });
    }

    private void displayUtility() {
        mUtilityNameEditText.setText(mUtility.name, TextView.BufferType.EDITABLE);
        mCompanyNameEditText.setText(mUtility.company_name, TextView.BufferType.EDITABLE);
        mCompanyWebsiteEditText.setText(mUtility.company_website, TextView.BufferType.EDITABLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_utility_editor, menu);

        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        if (TextUtils.isEmpty(mUtilityId)) {
            menu.findItem(R.id.action_delete).setVisible(false);
        }

        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_save:
                saveCurrentUtilityToFirebase();
                return true;
            case R.id.action_delete:
                confirmDelete();
                return true;
            case android.R.id.home:
                onBackPressed();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    private void confirmDelete() {
        DialogInterface.OnClickListener dialogClickListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                switch (which) {
                    case DialogInterface.BUTTON_POSITIVE:
                        deleteUtility(mUtilityId);
                        finish();
                        break;

                    case DialogInterface.BUTTON_NEGATIVE:
                        dialog.dismiss();
                        break;
                }
            }
        };

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.confirm_delete_utility)
                .setPositiveButton(R.string.button_yes, dialogClickListener)
                .setNegativeButton(R.string.button_no, dialogClickListener)
                .show();
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        mUtility.company_name = mCompanyNameEditText.getText().toString();
        mUtility.company_website = mCompanyWebsiteEditText.getText().toString();
        mUtility.name = mUtilityNameEditText.getText().toString();

        outState.putParcelable(UTILITY_KEY, mUtility);
    }
}
