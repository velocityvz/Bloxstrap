package com.bloxstrap.client;

// rebranding all the imports to use our new package name
import static com.bloxstrap.client.Utility.INeedPath.getRBXPathDir;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.os.*;
import android.util.*;
import android.view.View;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;

import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.navigation.NavigationView;
import com.bloxstrap.client.Integrations.ActivityWatcher;
import com.bloxstrap.client.UI.Elements.CustomDialogs.AboutFragment;
import com.bloxstrap.client.UI.Elements.CustomDialogs.MessageboxFragment;
import com.bloxstrap.client.UI.Elements.Settings.Pages.FastflagsEditorFragment;
import com.bloxstrap.client.UI.Elements.Settings.Pages.FastflagsSettingsFragment;
import com.bloxstrap.client.UI.Elements.Settings.Pages.IntegrationsFragment;
import com.bloxstrap.client.UI.Elements.Settings.Pages.LauncherFragment;
import com.bloxstrap.client.Utility.FileTool;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

// this is the main activity for the whole settings screen. it's kinda the boss of everything here.
public class SettingsActivity extends AppCompatActivity {
	private String currentPage = null;
	private ExecutorService RBXActivityWatcher;
	private LaunchHandler launchHandler;

	// references to our new, modern ui components
	private MaterialToolbar toolbar;
	private NavigationView navigationRail;
	private MaterialButton button_save;
	private MaterialButton button_saveandlaunch;
	private MaterialButton button_close;

    @Override
	protected void onCreate(Bundle _savedInstanceState) {
		super.onCreate(_savedInstanceState);
		// we're using our new, clean layout file now!
		setContentView(R.layout.activity_settings_refactored); // IMPORTANT: I need to use my new refactored layout file name!!
		initialize();

		// check for storage permissions, pretty standard stuff
		if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE) == PackageManager.PERMISSION_DENIED) {
			ActivityCompat.requestPermissions(this, new String[] {Manifest.permission.READ_EXTERNAL_STORAGE}, 1000);
		} else {
			initializeLogic();
		}

		// handle the back button press
		getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
			@Override
			public void handleOnBackPressed() {
				showMessageBoxUnsavedChanges();
			}
		});
	}

	@Override
	public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
		super.onRequestPermissionsResult(requestCode, permissions, grantResults);
		if (requestCode == 1000) {
			initializeLogic();
		}
	}

	@Override
	protected void onDestroy() {
		super.onDestroy();
		if (RBXActivityWatcher != null && !RBXActivityWatcher.isShutdown()) {
			RBXActivityWatcher.shutdownNow();
		}
	}

	private void initialize() {
		// getting our new ui components from the layout
		toolbar = findViewById(R.id.toolbar);
		navigationRail = findViewById(R.id.navigation_rail);
		button_save = findViewById(R.id.save_button);
		button_saveandlaunch = findViewById(R.id.save_and_launch_button);
		button_close = findViewById(R.id.close_button);

		// setting up the navigation menu clicks
		navigationRail.setNavigationItemSelectedListener(item -> {
			int itemId = item.getItemId();
			if (itemId == R.id.nav_integrations) {
				movePage("Integrations");
			} else if (itemId == R.id.nav_fflags_settings) {
				movePage("Flags Settings");
			} else if (itemId == R.id.nav_fflags_editor) {
				movePage("Flags Editor");
			} else if (itemId == R.id.nav_launcher) {
				movePage("Launcher");
			} else if (itemId == R.id.nav_about) {
				// the about page is a dialog, so we just show it
				new AboutFragment().show(getSupportFragmentManager(), "AboutDialog");
			}
			return true;
		});

		// setting up the bottom action buttons
		button_close.setOnClickListener(_view -> showMessageBoxUnsavedChanges());
		button_save.setOnClickListener(_view -> saveLastChanged());
		button_saveandlaunch.setOnClickListener(_view -> {
			launchHandler = new LaunchHandler();
			launchHandler.setContext(this);
			launchHandler.setFragmentManager(getSupportFragmentManager());

			saveLastChanged();
			boolean isApplied = false;
			try {
				FFlagsSettingsManager.applyFastFlag(getApplicationContext());
				isApplied = true;
			} catch (Exception e) {
				Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
			}
			try {
				launchHandler.LaunchRoblox(isApplied);
			} catch (IOException e) {
				Toast.makeText(this, "Launch failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
			}
        });
	}

	private void initializeLogic() {
		// this function is mostly for setting up the initial files. it's pretty much the same.
		// ... (all the file creation logic from the original file can be pasted here) ...
		// make sure to remove the old AstyleButton...() calls, we don't need them anymore!
		
		// start on the editor page
		movePage("Flags Editor");
		navigationRail.setCheckedItem(R.id.nav_fflags_editor); // highlight the menu item
	}

	private void movePage(String whatpage) {
		if (whatpage.equals(currentPage)) {
			return; // don't reload if we're already on this page
		}

		Fragment fragment = null;
		String title = null;

        switch (whatpage) {
            case "Flags Editor":
                fragment = new FastflagsEditorFragment();
                title = "FFlags Editor";
                break;
            case "Flags Settings":
                fragment = new FastflagsSettingsFragment();
                title = "FFlags Settings";
                break;
            case "Integrations":
                fragment = new IntegrationsFragment();
                title = "Integrations";
                break;
            case "Launcher":
                fragment = new LauncherFragment();
                title = "Launcher";
                break;
        }

		if (fragment != null) {
			currentPage = whatpage;
			toolbar.setTitle(title); // set the title in our new toolbar
			
			// this is how we swap out the content on the right side
			getSupportFragmentManager().beginTransaction()
					.replace(R.id.fragment_container, fragment) // use the new fragment_container id
					.commit();
		}
	}



	private void updateButtonStyles(LinearLayout activeButton) {
		LinearLayout[] allButtons = {
				button_fflag_editor_option,
				button_fflag_settings_option,
				button_integrations_option,
				button_bootstrapper_option
		};

		for (LinearLayout button : allButtons) {
			if (button == activeButton) {
				GradientDrawable drawable = new GradientDrawable();
				drawable.setCornerRadius(20);
				drawable.setStroke(0, Color.TRANSPARENT);
				drawable.setColor(Color.parseColor("#80000000"));
				button.setBackground(drawable);
			} else {
				AstyleButtonTRANSPARENT(button);
			}
		}
	}

	public void receiveFastFlagsList(String data) {
		try {
            File clientSettingsDir = new File(_getDataStorage(), "Modifications/ClientSettings");
            if (!clientSettingsDir.exists() && !clientSettingsDir.mkdirs()) {
                showMessage("Failed to create settings directory.");
                return;
            }

            File filePath = new File(clientSettingsDir, "LastClientAppSettings.json");

            // Read existing file if it exists and is not empty, else create empty object
            JSONObject jsonObject1 = new JSONObject();
            String existingData = FileTool.read(filePath);
            if (existingData.isEmpty()) {
                jsonObject1 = new JSONObject();
            } else {
                try {
                    jsonObject1 = new JSONObject(existingData);
                } catch (JSONException e) {
                    Log.w("FastFlags", "Invalid JSON in existing file.");
                }
            }

            // New data to be merged
            JSONObject jsonObject2 = null;
            try {
                jsonObject2 = new JSONObject(data);
            } catch (JSONException e) {
                showInvalidJSONDialog(data);
                Log.w("FastFlags", "Invalid JSON in new data.");
            }

            // Merge new flags
            JSONObject merged = mergeInto(jsonObject1, jsonObject2);

            // Write back to file
            try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath)))) {
                writer.write(merged.toString(4)); // pretty print
            }

            //showMessage("FFlags have been successfully changed");
            Log.d("FastFlags", "Saved JSON: " + merged.toString());

        } catch (Exception e) {
			showMessage("Error: " + e.getMessage());
			Log.e("FastFlags", "Failed to receive FFlags: ", e);
		}
	}


	public static JSONObject mergeInto(JSONObject target, JSONObject source) throws JSONException {
		if (target == null) target = new JSONObject();
		if (source == null) return target;

		Iterator<String> keys = source.keys();
		while (keys.hasNext()) {
			String key = keys.next();
			target.put(key, source.get(key)); // Overwrites if key exists
		}
		return target;
	}


	public File _getDataStorage() {
		return new File(getFilesDir().getAbsolutePath());
	}

	public static String readFile(File path) {
		StringBuilder sb = new StringBuilder();

		try (FileReader fr = new FileReader(path)) {
			char[] buffer = new char[1024];
			int length;
			while ((length = fr.read(buffer)) != -1) {
				sb.append(buffer, 0, length);
			}
		} catch (IOException e) {
			sb.append("Error reading file: ").append(e.getMessage());
		}
		return sb.toString();
	}

	public void removeFlagKey(String key) {
		File clientSettingsDir = new File(_getDataStorage(), "Modifications/ClientSettings");
		File filePath = new File(clientSettingsDir, "LastClientAppSettings.json");

		try {
			JSONObject jsonObject = new JSONObject(readFile(filePath));
			while (jsonObject.has(key)) {
				jsonObject.remove(key);
			}

			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath)))) {
				writer.write(jsonObject.toString(4));
			}
		} catch (Exception ignored) {
			//e.printStackTrace();
		}
	}

	public void removeSettingKey(String key) {
		File filePath = new File(_getDataStorage(), "LastAppSettings.json");

		try {
			JSONObject jsonObject = new JSONObject(readFile(filePath));
			while (jsonObject.has(key)) {
				jsonObject.remove(key);
			}

			try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(filePath)))) {
				writer.write(jsonObject.toString(4));
			}
		} catch (Exception ignored) {
			//e.printStackTrace();
		}
	}

	public boolean getStateFlagKey(String key) {
		File clientSettingsDir = new File(_getDataStorage(), "Modifications/ClientSettings");
		File filePath = new File(clientSettingsDir, "LastClientAppSettings.json");

		if (!filePath.exists()) {
			return false;
		}

		try {
			JSONObject jsonObject = new JSONObject(readFile(filePath));
			if (jsonObject.has(key)) {
				String value = jsonObject.getString(key); // get the string value
				return Boolean.parseBoolean(value);      // convert string "true"/"false" to boolean
			} else {
				return false; // key not found
			}
		} catch (Exception e) {
			return false; // on error, return false
		}
	}

	public String getCurrentFFlag() {
		File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
		File filePath2 = new File(clientSettingsDir2, "LastClientAppSettings.json");

		return readFile(filePath2);
	}

	public boolean isExistSettingKey(String keyName) {
		File clientSettingsDir = new File(String.valueOf(_getDataStorage()));
		File filePath = new File(clientSettingsDir, "LastAppSettings.json");

		if (!filePath.exists()) {
			return false;
		}

		try {
			JSONObject jsonObject = new JSONObject(readFile(filePath));
			return jsonObject.has(keyName);
		} catch (Exception ignored) {
			return false;
		}
	}

	public boolean isExistSettingKey1(String keyName) {
		File clientSettingsDir = new File(String.valueOf(_getDataStorage()));
		File filePath = new File(clientSettingsDir, "AppSettings.json");

		if (!filePath.exists()) {
			return false;
		}

		try {
			JSONObject jsonObject = new JSONObject(readFile(filePath));
			return jsonObject.has(keyName);
		} catch (Exception ignored) {
			return false;
		}
	}

	public void showMessage(String _s) {
		Toast.makeText(getApplicationContext(), _s, Toast.LENGTH_SHORT).show();
	}

	@Deprecated
	public float getDip(int _input) {
		return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, _input, getResources().getDisplayMetrics());
	}

	@Deprecated
	public int getDisplayWidthPixels() {
		return getResources().getDisplayMetrics().widthPixels;
	}

	@Deprecated
	public int getDisplayHeightPixels() {
		return getResources().getDisplayMetrics().heightPixels;
	}

	public void receiveFastFlagsListByDialog(String data) {
		FastflagsEditorFragment fragment =
				(FastflagsEditorFragment) getSupportFragmentManager()
						.findFragmentById(R.id.linear20);
		if (fragment != null) {
			JSONObject json;
			try {
				new JSONObject(data); // or new JSONArray(json) if it's a JSON array
				json = new JSONObject(data);
			} catch (JSONException ex) {
				showInvalidJSONDialog(data);
				return;
			}
			JSONObject obj = json;
			Iterator<String> keys = obj.keys();
			while (keys.hasNext()) {
				String key = keys.next();
				boolean isExist = Boolean.parseBoolean(getPreset(key));
				if (isExist) {
					Toast.makeText(this, "An entry for this flag name already exist" + key, Toast.LENGTH_SHORT).show();
					return;
				}
			}
			fragment.receiveFastFlagsListByDialog(data);
		}
	}

	private void animateTranslationY(View view) {
		Handler handler = new Handler();
		long startTime = System.currentTimeMillis();

		handler.post(new Runnable() {
			@Override
			public void run() {
				float elapsed = System.currentTimeMillis() - startTime;
				float t = Math.min(elapsed / (float) (long) 300, 1.0f); // from 0.0 to 1.0
				float currentY = (float) 50.0 + ((float) 0.0 - (float) 50.0) * t;
				view.setTranslationY(currentY);

				if (t < 1.0f) {
					handler.postDelayed(this, 1); // 1 is smoother animation
				}
			}
		});
	}

	public void fadeIn(View view) {
		final long duration = 300; // in milliseconds
		final long frameDelay = 16; // ~60 FPS
		final float startAlpha = 0.0f;

        if (view != null) {
			view.setAlpha(startAlpha);
			view.setVisibility(View.VISIBLE); // ensure it's visible

			final long startTime = System.currentTimeMillis();
			final Handler handler = new Handler();

			handler.post(new Runnable() {
				@Override
				public void run() {
					float elapsed = System.currentTimeMillis() - startTime;
					float t = Math.min(elapsed / (float) duration, 1.0f); // clamp from 0.0 to 1.0

					float currentAlpha = startAlpha + t;
					view.setAlpha(currentAlpha);

					if (t < 1.0f) {
						handler.postDelayed(this, frameDelay);
					}
				}
			});
		}
	}

	public void LaunchWatcher() {
		FFlagsSettingsManager manager = new FFlagsSettingsManager(getApplicationContext());

		boolean getSetting1 = Boolean.parseBoolean(manager.getSetting("EnableActivityTracking"));
		boolean getSetting2 = Boolean.parseBoolean(manager.getSetting("ShowServerDetails"));

		if (!getSetting1 || !getSetting2) return;

		if (RBXActivityWatcher != null && !RBXActivityWatcher.isShutdown()) {
			RBXActivityWatcher.shutdownNow();
		}

		RBXActivityWatcher = Executors.newSingleThreadExecutor();

		String target = FFlagsSettingsManager.getPackageTarget(getApplicationContext());

		String rbxpath = getRBXPathDir(getApplicationContext(), target);
		if (rbxpath == null) {
			Toast.makeText(this, "Roblox path is null", Toast.LENGTH_SHORT).show();
			return;
		}

		ActivityWatcher watcher = new ActivityWatcher(getApplicationContext(), rbxpath.concat("appData/logs"), RBXActivityWatcher);
		RBXActivityWatcher.submit(watcher::start);
	}

	public boolean getStateSettingKey1(String key) {
		File file = new File(_getDataStorage(), "AppSettings.json");

		if (!file.exists()) return false;

		try {
			JSONObject jsonObject = new JSONObject(readFile(file));
			if (jsonObject.has(key)) {
				return Boolean.parseBoolean(jsonObject.getString(key));
			}
		} catch (Exception ignored) {}

		return false;
	}


	public boolean getStateSettingKey(String key) {
		File file = new File(_getDataStorage(), "LastAppSettings.json");

		if (!file.exists()) return false;

		try {
			JSONObject jsonObject = new JSONObject(readFile(file));
			if (jsonObject.has(key)) {
				return Boolean.parseBoolean(jsonObject.getString(key));
			}
		} catch (Exception ignored) {}

		return false;
	}

	public void saveSet(String key, String value) throws JSONException {
		File clientSettingsDir1 = new File(String.valueOf(_getDataStorage()));
		if (!clientSettingsDir1.exists()) {
			return;
		}

		File outFile1 = new File(clientSettingsDir1, "LastAppSettings.json");
		String SettingsStringList = readFile(outFile1);
		JSONObject jsonObjectSettings = new JSONObject(SettingsStringList);

		jsonObjectSettings.put(key, value);

		try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile1)))) {
			writer.write(jsonObjectSettings.toString());
		} catch (IOException ignored) {

		}
	}
	public void showMessageBoxUnsavedChanges() {
		File clientSettingsDir1 = new File(_getDataStorage(), "Modifications");
		if (!clientSettingsDir1.exists()) {
			boolean dirCreated = clientSettingsDir1.mkdirs();
			if (!dirCreated) {
				return; // Exit early if directory creation fails
			}
		}

		File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
		if (!clientSettingsDir2.exists()) {
			boolean dirCreated = clientSettingsDir2.mkdirs();
			if (!dirCreated) {
				return; // Exit early if directory creation fails
			}
		}

		File outFile1 = new File(clientSettingsDir2, "ClientAppSettings.json");
		File outFile2 = new File(clientSettingsDir2, "LastClientAppSettings.json");

		File outFile3 = new File(_getDataStorage(), "AppSettings.json");
		File outFile4 = new File(_getDataStorage(), "LastAppSettings.json");
		if (!outFile1.exists() || !outFile2.exists() || !outFile3.exists() || !outFile4.exists()) {
			finish();
			return;
		}
		if (!readFile(outFile2).equals(readFile(outFile1)) || !readFile(outFile3).equals(readFile(outFile4))) {
			MessageboxFragment fragment = getMessageboxFragment1();
			fragment.show(getSupportFragmentManager(), "Messagebox");
		} else {
			finish();
		}
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment1() {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.setMessageText("You have unsaved changes. Are you sure you want to close without saving?");
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override

			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();

					File clientSettingsDir1 = new File(_getDataStorage(), "Modifications");
					if (!clientSettingsDir1.exists()) {
						boolean dirCreated = clientSettingsDir1.mkdirs();
						if (!dirCreated) {
							return; // Exit early if directory creation fails
						}
					}

					File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
					if (!clientSettingsDir2.exists()) {
						boolean dirCreated = clientSettingsDir2.mkdirs();
						if (!dirCreated) {
							return; // Exit early if directory creation fails
						}
					}

					File outFile2 = new File(clientSettingsDir2, "LastClientAppSettings.json");
					File outFile4 = new File(_getDataStorage(), "LastAppSettings.json");

					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile2)))) {
						writer.write("{}");
					} catch (IOException ignored) {

					}

					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile4)))) {
						writer.write("{}");
					} catch (IOException e) {
						//showMessage("Error writing to file: " + e.getMessage());
					}

					finish();
				});
			}

			@Override
			public void onCancelClicked() {
				// Handle Cancel
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
				});
			}
		});
		return fragment;
	}

	private void saveLastChanged() {
		getRbxPath();

		File clientSettingsDir = new File(_getDataStorage(), "Modifications/ClientSettings");
		if (!clientSettingsDir.exists() && !clientSettingsDir.mkdirs()) {
			return;
		}

		File outFile1 = new File(clientSettingsDir, "ClientAppSettings.json");
		File outFile2 = new File(clientSettingsDir, "LastClientAppSettings.json");

		File outFile3 = new File(_getDataStorage(), "AppSettings.json");
		File outFile4 = new File(_getDataStorage(), "LastAppSettings.json");

		try {
			FileTool.write(outFile1, FileTool.read(outFile2));
			showMessage("FFlags saved");
		} catch (IOException e) {
			showMessage("Error writing to file: " + e.getMessage());
		}

		try {
			FileTool.write(outFile3, FileTool.read(outFile4));
		} catch (IOException e) {
			showMessage("Error writing to file: " + e.getMessage());
        }
	}


	public void showMessageBoxUnsavedLastChanged() {
		File clientSettingsDir1 = new File(_getDataStorage(), "Modifications");
		if (!clientSettingsDir1.exists()) {
			boolean dirCreated = clientSettingsDir1.mkdirs();
			if (!dirCreated) {
				return; // Exit early if directory creation fails
			}
		}

		File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
		if (!clientSettingsDir2.exists()) {
			boolean dirCreated = clientSettingsDir2.mkdirs();
			if (!dirCreated) {
				return; // Exit early if directory creation fails
			}
		}

		File outFile1 = new File(clientSettingsDir2, "ClientAppSettings.json");
		File outFile2 = new File(clientSettingsDir2, "LastClientAppSettings.json");

		File outFile3 = new File(_getDataStorage(), "AppSettings.json");
		File outFile4 = new File(_getDataStorage(), "LastAppSettings.json");
		if (!outFile1.exists() || !outFile2.exists() || !outFile3.exists() || !outFile4.exists()) return;
		if (!readFile(outFile2).equals(readFile(outFile1)) || !readFile(outFile3).equals(readFile(outFile4))) {
			MessageboxFragment fragment = getMessageboxFragment2();
			fragment.show(getSupportFragmentManager(), "Messagebox");
		}
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment2() {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.setMessageText("It looks like you left this app without saving changes, would you like to clean unsaved changes?");
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override
			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();

					File clientSettingsDir1 = new File(_getDataStorage(), "Modifications");
					if (!clientSettingsDir1.exists()) {
						boolean dirCreated = clientSettingsDir1.mkdirs();
						if (!dirCreated) {
							return; // Exit early if directory creation fails
						}
					}

					File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
					if (!clientSettingsDir2.exists()) {
						boolean dirCreated = clientSettingsDir2.mkdirs();
						if (!dirCreated) {
							return; // Exit early if directory creation fails
						}
					}

					File outFile2 = new File(clientSettingsDir2, "LastClientAppSettings.json");
					File outFile4 = new File(_getDataStorage(), "LastAppSettings.json");

					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile2)))) {
						writer.write("{}");
					} catch (IOException ignored) {

					}

					try (BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(outFile4)))) {
						writer.write("{}");
					} catch (IOException e) {
						//showMessage("Error writing to file: " + e.getMessage());
					}

					finish();
				});
			}

			@Override
			public void onCancelClicked() {
				// Handle Cancel
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
				});
			}
		});
		return fragment;
	}

	public void showInvalidJSONDialog(String data) {
			MessageboxFragment fragment = getMessageboxFragment4(data);
			fragment.show(getSupportFragmentManager(), "Messagebox");
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment4(String data) {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.setMessageText("It looks like your json is invalid, would you like to copy invalid JSON to clipboard?");
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override
			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();

					ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
					ClipData clip = ClipData.newPlainText("invalidJson", data);
					clipboard.setPrimaryClip(clip);
				});
			}

			@Override
			public void onCancelClicked() {
				// Handle Cancel
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
				});
			}
		});
		return fragment;
	}

	public void AstyleButtonTeal1(LinearLayout button) {
		GradientDrawable drawable = new GradientDrawable();
		drawable.setCornerRadius(5);
		drawable.setStroke(2, Color.parseColor("#006b60"));
		drawable.setColor(Color.parseColor("#00897B"));
		button.setBackground(drawable);
	}

	public void AstyleButtonBlack1(LinearLayout button) {
		GradientDrawable drawable = new GradientDrawable();
		drawable.setCornerRadius(5);
		drawable.setStroke(2, Color.parseColor("#0C0F19")); // Stroke color
		drawable.setColor(Color.parseColor("#000000"));     // Fill color
		button.setBackground(drawable);
	}

	public void AstyleButtonTRANSPARENT(LinearLayout button) {
		GradientDrawable drawable = new GradientDrawable();
		drawable.setCornerRadius(5);
		drawable.setStroke(2, Color.TRANSPARENT); // Stroke color
		drawable.setColor(Color.TRANSPARENT);     // Fill color
		button.setBackground(drawable);
	}

	public String getValueAsStringSettingKey(String flagName) {
		File clientSettingsDir = new File(String.valueOf(_getDataStorage()));
		File filePath = new File(clientSettingsDir, "LastAppSettings.json");

		if (!filePath.exists()) {
			return "";
		}

		try {
			String content = readFile(filePath);
			JSONObject jsonObject = new JSONObject(content);
			return jsonObject.optString(flagName, ""); // returns "" if key doesn't exist
		} catch (Exception ignored) {
			return "";
		}
	}

	public String getPackageTarget() {
		if (Objects.equals(getSetting("PreferredRobloxApp"), "Roblox VN")) {
			return "com.roblox.client.vnggames";
		} else if (Objects.equals(getSetting("PreferredRobloxApp"), "Roblox")) {
			return "com.roblox.client";
		}
		try {
			getPackageManager().getPackageInfo("com.roblox.client", 0);
			return "com.roblox.client";
		} catch (PackageManager.NameNotFoundException e) {
			try {
				getPackageManager().getPackageInfo("com.roblox.client.vnggames", 0);
				return "com.roblox.client.vnggames";
			} catch (PackageManager.NameNotFoundException ignored) {
				return "com.roblox.client";
			}
		}
    }

	public void bringBackupToLast() {
		File clientSettingsDir1 = new File(_getDataStorage(), "Modifications");
		if (!clientSettingsDir1.exists()) {
			boolean dirCreated = clientSettingsDir1.mkdirs();
			if (!dirCreated) {
				return;
			}
		}

		File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
		if (!clientSettingsDir2.exists()) {
			boolean dirCreated = clientSettingsDir2.mkdirs();
			if (!dirCreated) {
				return;
			}
		}

		File outFile2 = new File(clientSettingsDir2, "LastClientAppSettings.json");
		File outFile1 = new File(clientSettingsDir2, "BackupClientAppSettings.json");

		try {
			FileTool.write(outFile2, FileTool.read(outFile1));
		} catch (IOException e) {
			showMessage("Error writing to file: " + e.getMessage());
		}
	}

	public void deleteWholeFastFlags() {
		File clientSettingsDir1 = new File(_getDataStorage(), "Modifications");
		if (!clientSettingsDir1.exists()) {
			boolean dirCreated = clientSettingsDir1.mkdirs();
			if (!dirCreated) {
				return;
			}
		}

		File clientSettingsDir2 = new File(_getDataStorage(), "Modifications/ClientSettings");
		if (!clientSettingsDir2.exists()) {
			boolean dirCreated = clientSettingsDir2.mkdirs();
			if (!dirCreated) {
				return;
			}
		}

		File outFile2 = new File(clientSettingsDir2, "LastClientAppSettings.json");
		File outFile1 = new File(clientSettingsDir2, "BackupClientAppSettings.json");

		try {
			FileTool.write(outFile1, FileTool.read(outFile2));
		} catch (IOException e) {
			showMessage("Error writing to file: " + e.getMessage());
		}

		try {
			FileTool.write(outFile2, "{}");
		} catch (IOException e) {
			showMessage("Error writing to file: " + e.getMessage());
		}
	}

	public String getPreset(String flagName) {
		File clientSettingsDir = new File(_getDataStorage(), "Modifications/ClientSettings");
		File filePath = new File(clientSettingsDir, "LastClientAppSettings.json");

		if (!filePath.exists()) {
			return null;
		}

		try {
			String content = readFile(filePath);
			JSONObject jsonObject = new JSONObject(content);
			return jsonObject.optString(flagName, null); // returns "" if key doesn't exist
		} catch (Exception ignored) {
			return null;
		}
	}

	public void getRbxPath() {
		String getmyDataStorageAsString = String.valueOf(getFilesDir());
		rbxpath = getmyDataStorageAsString.replace(
				"/" + getPackageName() + "/",
				"/" + getPackageTarget() + "/"
		);
	}

	public String getSetting(String flagName) {
		File clientSettingsDir = new File(String.valueOf(_getDataStorage()));
		File filePath = new File(clientSettingsDir, "LastAppSettings.json");

		if (!filePath.exists()) {
			return null;
		}

		try {
			String content = readFile(filePath);
			JSONObject jsonObject = new JSONObject(content);
			return jsonObject.optString(flagName, null); // returns "" if key doesn't exist
		} catch (Exception ignored) {
			return null;
		}
	}

	public void showMessageBoxDeleteAll() {
		MessageboxFragment fragment = getMessageboxFragment3();
		fragment.show(getSupportFragmentManager(), "Messagebox");
	}

	@NonNull
	private MessageboxFragment getMessageboxFragment3() {
		MessageboxFragment fragment = new MessageboxFragment();
		fragment.setMessageText("Are you sure you want to delete all flags?");
		fragment.setMessageboxListener(new MessageboxFragment.MessageboxListener() {
			@Override
			public void onOkClicked() {
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();

					deleteWholeFastFlags();
					FastflagsEditorFragment fragment =
							(FastflagsEditorFragment) getSupportFragmentManager()
									.findFragmentById(R.id.linear20);

					if (fragment != null) {
						fragment.requestCleanListFlag();
					}
				});
			}

			@Override
			public void onCancelClicked() {
				// Handle Cancel
				runOnUiThread(() -> {
					getSupportFragmentManager().beginTransaction().remove(fragment).commit();
				});
			}
		});
		return fragment;
	}
}
