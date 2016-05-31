package de.blau.android.propertyeditor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnShowListener;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AlertDialog.Builder;
import android.support.v7.widget.AppCompatCheckBox;
import android.support.v7.widget.AppCompatRadioButton;
import android.text.TextUtils.TruncateAt;
import android.util.AttributeSet;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.CompoundButton;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import de.blau.android.Application;
import de.blau.android.HelpViewer;
import de.blau.android.R;
import de.blau.android.names.Names;
import de.blau.android.names.Names.NameAndTags;
import de.blau.android.osm.OsmElement;
import de.blau.android.osm.Tags;
import de.blau.android.prefs.Preferences;
import de.blau.android.presets.Preset;
import de.blau.android.presets.Preset.PresetItem;
import de.blau.android.presets.Preset.PresetKeyType;
import de.blau.android.presets.ValueWithCount;
import de.blau.android.util.BaseFragment;
import de.blau.android.util.NetworkStatus;
import de.blau.android.util.StringWithDescription;
import de.blau.android.util.ThemeUtils;
import de.blau.android.util.Util;
import de.blau.android.views.CustomAutoCompleteTextView;


	
public class TagFormFragment extends BaseFragment implements FormUpdate {

	private static final String DEBUG_TAG = TagFormFragment.class.getSimpleName();
	
	/**
	 * Max number of entries before we use a normal dropdown list for selection
	 */
	private static final int MAX_ENTRIES_MULTISELECT = 8;
	
	LayoutInflater inflater = null;

	private Names names = null;

	private Preferences prefs = null;

	private EditorUpdate tagListener = null;

	private NameAdapters nameAdapters = null;
	
	private boolean focusOnAddress = false;
	
	private int maxInlineValues = 3;

	
	/**
	 * @param applyLastAddressTags 
	 * @param focusOnKey 
	 * @param displayMRUpresets 
     */
    static public TagFormFragment newInstance(boolean displayMRUpresets, boolean focusOnAddress) {
    	TagFormFragment f = new TagFormFragment();
    	
        Bundle args = new Bundle();
   
        args.putSerializable("displayMRUpresets", Boolean.valueOf(displayMRUpresets));
        args.putSerializable("focusOnAddress", Boolean.valueOf(focusOnAddress));

        f.setArguments(args);
        // f.setShowsDialog(true);
        
        return f;
    }
    
    @Override
    public void onAttachToContext(Context context) {
        Log.d(DEBUG_TAG, "onAttachToContext");
        try {
        	tagListener = (EditorUpdate) context;
            nameAdapters = (NameAdapters) context;
        } catch (ClassCastException e) {
        	throw new ClassCastException(context.toString() + " must implement OnPresetSelectedListener and NameAdapters");
        }
        setHasOptionsMenu(true);
        getActivity().supportInvalidateOptionsMenu();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
       	Log.d(DEBUG_TAG, "onCreate");
    }
	
	/** 
	 * display member elements of the relation if any
	 * @param members 
	 */
	@Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
            Bundle savedInstanceState) {
    	ScrollView rowLayout = null;

		if (savedInstanceState == null) {
			// No previous state to restore - get the state from the intent
			Log.d(DEBUG_TAG, "Initializing from original arguments");
		} else {
			// Restore activity from saved state
			Log.d(DEBUG_TAG, "Restoring from savedInstanceState");
		}
    	
    	
     	this.inflater = inflater;
     	rowLayout = (ScrollView) inflater.inflate(R.layout.tag_form_view, container, false);
           	
     	boolean displayMRUpresets = ((Boolean) getArguments().getSerializable("displayMRUpresets")).booleanValue();
     	focusOnAddress = ((Boolean) getArguments().getSerializable("focusOnAddress")).booleanValue();
     	
       	// Log.d(DEBUG_TAG,"element " + element + " tags " + tags);
		
	
		if (getUserVisibleHint()) { // don't request focus if we are not visible 
			Log.d(DEBUG_TAG,"is visible");
		}	
		// 
    	prefs = new Preferences(getActivity());
		
		if (prefs.getEnableNameSuggestions()) {
			names = Application.getNames(getActivity());
		}
		
		maxInlineValues = prefs.getMaxInlineValues();

		if (displayMRUpresets) {
			Log.d(DEBUG_TAG,"Adding MRU prests");
			FragmentManager fm = getChildFragmentManager();
			FragmentTransaction ft = fm.beginTransaction();
			Fragment recentPresetsFragment = fm.findFragmentByTag("recentpresets_fragment");
			if (recentPresetsFragment != null) {
				ft.remove(recentPresetsFragment);
			}
			
			recentPresetsFragment = RecentPresetsFragment.newInstance(((PropertyEditor)getActivity()).getElement()); // FIXME
			ft.add(R.id.form_mru_layout,recentPresetsFragment,"recentpresets_fragment");
			ft.commit();
		}
		
		Log.d(DEBUG_TAG,"onCreateView returning");
		return rowLayout;
	}
    
    
    @Override
    public void onStart() {
    	super.onStart();
    	Log.d(DEBUG_TAG, "onStart");
    }
    
    @Override
    public void onResume() {
    	super.onResume();
    	Log.d(DEBUG_TAG, "onResume");
    }

	@Override
    public void onSaveInstanceState(Bundle outState) {
    	super.onSaveInstanceState(outState);
    	Log.d(DEBUG_TAG, "onSaveInstanceState");
    }  
    
    @Override
    public void onPause() {
    	super.onPause();
    	Log.d(DEBUG_TAG, "onPause");
    }
       
    @Override
    public void onStop() {
    	super.onStop();
    	Log.d(DEBUG_TAG, "onStop");
    }
    
    @Override
    public void onDestroy() {
    	super.onDestroy();
    	Log.d(DEBUG_TAG, "onDestroy");
    }
    
    @Override
    public void onDestroyView() {
    	super.onDestroyView();
    	Log.d(DEBUG_TAG, "onDestroyView");
    }


	
	/**
	 * Simpilified version for non-multi-select and preset only situation
	 * @param key
	 * @param value
	 * @param allTags
	 * @return
	 */
	protected ArrayAdapter<?> getValueAutocompleteAdapter(String key, ArrayList<String> values, PresetItem preset, LinkedHashMap<String, String> allTags) {
		ArrayAdapter<?> adapter = null;
	
		if (key != null && key.length() > 0) {
			Set<String> usedKeys = allTags.keySet();
			if (TagEditorFragment.isStreetName(key, usedKeys)) {
				adapter = nameAdapters.getStreetNameAdapter(values);
			} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
				adapter = nameAdapters.getPlaceNameAdapter(values);
			} else if (key.equals(Tags.KEY_NAME) && (names != null) && TagEditorFragment.useNameSuggestions(usedKeys)) {
				Log.d(DEBUG_TAG,"generate suggestions for name from name suggestion index");
				ArrayList<NameAndTags> suggestions = (ArrayList<NameAndTags>) names.getNames(new TreeMap<String,String>(allTags)); 
				if (suggestions != null && !suggestions.isEmpty()) {
					ArrayList<NameAndTags> result = suggestions;
					Collections.sort(result);
					adapter = new ArrayAdapter<NameAndTags>(getActivity(), R.layout.autocomplete_row, result);
				}
			} else {
				HashMap<String, Integer> counter = new HashMap<String, Integer>();
				ArrayAdapter<ValueWithCount> adapter2 = new ArrayAdapter<ValueWithCount>(getActivity(), R.layout.autocomplete_row);
	
				if (preset != null) {
				Collection<StringWithDescription> presetValues = preset.getAutocompleteValues(key);
				Log.d(DEBUG_TAG,"setting autocomplete adapter for values " + presetValues);
				if (values != null && !values.isEmpty()) {
					ArrayList<StringWithDescription> result = new ArrayList<StringWithDescription>(presetValues);
					if (preset.sortIt(key)) {
						Collections.sort(result);
					}
					for (StringWithDescription s:result) {
						if (counter != null && counter.containsKey(s.getValue())) {
							continue; // skip stuff that is already listed
						}
						counter.put(s.getValue(),Integer.valueOf(1));
						adapter2.add(new ValueWithCount(s.getValue(), s.getDescription(), true));
					}
					Log.d(DEBUG_TAG,"key " + key + " type " + preset.getKeyType(key));
				} 
				} else {
					OsmElement element = ((PropertyEditor)getActivity()).getElement();
					if (((PropertyEditor)getActivity()).presets != null && element != null) { 
						Log.d(DEBUG_TAG,"generate suggestions for >" + key + "< from presets"); // only do this if there is no other source of suggestions
						for (StringWithDescription s:Preset.getAutocompleteValues(((PropertyEditor)getActivity()).presets,element.getType(), key)) {
							adapter2.add(new ValueWithCount(s.getValue(), s.getDescription()));
						}	
					}
				}
				if (!counter.containsKey("") && !counter.containsKey(null)) { // add empty value so that we can remove tag
					adapter2.insert(new ValueWithCount("", getString(R.string.tag_not_set), true),0); // FIXME allow unset value depending on preset
				}
				if (values != null) { // add in any non-standard non-empty values
					for (String value:values) {
						if (!"".equals(value) && !counter.containsKey(value)) {
							ValueWithCount v = new ValueWithCount(value,1); // FIXME determine description in some way
							adapter2.insert(v,0);
						}
					}
				}	
				Log.d(DEBUG_TAG,adapter2==null ? "adapter2 is null": "adapter2 has " + adapter2.getCount() + " elements");
				if (adapter2.getCount() > 0) {
					return adapter2;
				}

			}
		}
		Log.d(DEBUG_TAG,adapter==null ? "adapter is null": "adapter has " + adapter.getCount() + " elements");
		return adapter;
	}
	
	private ArrayList<String> splitValues(ArrayList<String>values, PresetItem preset, String key) {
		ArrayList<String> result = new ArrayList<String>();
		String delimiter = "" + preset.getDelimiter(key);
		if (values==null) {
			return null;
		}
		for (String v:values) {
			if (v==null) {
				return null;
			}
			for (String s:v.split(delimiter)) {
				result.add(s.trim());
			}
		}
		return result;
	}
	
	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		// final MenuInflater inflater = getSupportMenuInflater();
		super.onCreateOptionsMenu(menu, inflater);
		inflater.inflate(R.menu.tag_form_menu, menu);
		menu.findItem(R.id.tag_menu_mapfeatures).setEnabled(NetworkStatus.isConnected(getActivity()));
	}
	
	
	@Override
	public void onPrepareOptionsMenu(Menu menu) {
		super.onPrepareOptionsMenu(menu);
		// disable address tagging for stuff that won't have an address
		// menu.findItem(R.id.tag_menu_address).setVisible(!type.equals(Way.NAME) || element.hasTagKey(Tags.KEY_BUILDING));
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
			Log.d(DEBUG_TAG,"home pressed");
			updateEditorFromText();
			((PropertyEditor)getActivity()).sendResultAndFinish();
			return true;
		case R.id.tag_menu_address:
			updateEditorFromText();
			tagListener.predictAddressTags(true);
			update();
			if (!focusOnTag(Tags.KEY_ADDR_HOUSENUMBER)) {
				focusOnTag(Tags.KEY_ADDR_STREET);
			} 
			return true;
		case R.id.tag_menu_apply_preset:
			PresetItem pi = tagListener.getBestPreset();
			if (pi!=null) {
				((PropertyEditor)getActivity()).onPresetSelected(pi, true);
			}
			return true;
		case R.id.tag_menu_revert:
			doRevert();
			return true;
		case R.id.tag_menu_mapfeatures:
			startActivity(Preset.getMapFeaturesIntent(getActivity(),tagListener.getBestPreset()));
			return true;
//		case R.id.tag_menu_delete_unassociated_tags:
//			// remove tags that don't belong to an identified preset
//			return true;
		case R.id.tag_menu_resetMRU:
			for (Preset p:((PropertyEditor)getActivity()).presets)
				p.resetRecentlyUsed();
			((PropertyEditor)getActivity()).recreateRecentPresetView();
			return true;
		case R.id.tag_menu_reset_address_prediction:
			// simply overwrite with an empty file
			Address.resetLastAddresses(getActivity());
			return true;
		case R.id.tag_menu_help:
			HelpViewer.start(getActivity(), R.string.help_propertyeditor);
			return true;
		}		
		return false;
	}

	/**
	 * reload original arguments
	 */
	private void doRevert() {
		tagListener.revertTags();
		update();
	}
	
	/**
	 * update editor with any potential text changes that haven't been saved yet
	 */
	private boolean updateEditorFromText() {
		Log.d(DEBUG_TAG,"updating data from last text field");
		// check for focus on text field
		View fragementView = getView();
		if (fragementView == null) {
			return false; // already destroyed?
		}
		LinearLayout l = (LinearLayout) fragementView.findViewById(R.id.form_container_layout);
		if (l != null) { // FIXME this might need an alert
			View v = l.findFocus();
			Log.d(DEBUG_TAG,"focus is on " + v);
			if (v != null && v instanceof CustomAutoCompleteTextView){
				View row = v;
				do {
					row = (View) row.getParent();
				} while (row != null && !(row instanceof TagTextRow));
				if (row != null) {
					tagListener.updateSingleValue(((TagTextRow) row).getKey(), ((TagTextRow) row).getValue());
				}
			}
		}
		return true;
	}
	
	/**
	 * Return the view we have our rows in and work around some android craziness
	 * @return
	 */
	public View getImmutableView() {
		// android.support.v4.app.NoSaveStateFrameLayout
		View v =  getView();	
		if (v != null) {
			if ( v.getId() == R.id.form_immutable_row_layout) {
				Log.d(DEBUG_TAG,"got correct view in getView");
				return v;
			} else {
				v = v.findViewById(R.id.form_immutable_row_layout);
				if (v == null) {
					Log.d(DEBUG_TAG,"didn't find R.id.form_immutable_row_layout");
				}  else {
					Log.d(DEBUG_TAG,"Found R.id.form_immutable_row_layout");
				}
				return v;
			}
		} else {
			Log.d(DEBUG_TAG,"got null view in getView");
		}
		return null;
	}
	

	public void enableRecentPresets() {
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).enable();
		}
	}
	
	public void disableRecentPresets() {
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).disable();
		}
	}
	
	protected void recreateRecentPresetView() {
		Log.d(DEBUG_TAG,"Updating MRU prests");
		FragmentManager fm = getChildFragmentManager();
		Fragment recentPresetsFragment = fm.findFragmentByTag(PropertyEditor.RECENTPRESETS_FRAGMENT);
		if (recentPresetsFragment != null) {
			((RecentPresetsFragment)recentPresetsFragment).recreateRecentPresetView();
		}
	}
	
	public void update() {
		Log.d(DEBUG_TAG,"update");
		// remove all editable stuff
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			while (ll.getChildAt(0) instanceof EditableLayout) {
				ll.removeViewAt(0);
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return;
		}		
		final EditableLayout editableView  = (EditableLayout)inflater.inflate(R.layout.tag_form_editable, ll, false);
		editableView.setSaveEnabled(false); 
		int pos = 0;
		ll.addView(editableView, pos++);
		
		LinearLayout nonEditableView = (LinearLayout) getImmutableView();
		if (nonEditableView != null && nonEditableView.getChildCount() > 0) {
			nonEditableView.removeAllViews(); 
		}
		
    	PresetItem mainPreset = tagListener.getBestPreset();
    	editableView.setTitle(mainPreset);
    	
    	LinkedHashMap<String, String> allTags = tagListener.getKeyValueMapSingle(true);
    	Map<String, String> nonEditable;
    	if (mainPreset != null) {
    		nonEditable = addTagsToViews(editableView, mainPreset, allTags);
    		for (PresetItem preset:tagListener.getSecondaryPresets()) {
    			final EditableLayout editableView1  = (EditableLayout)inflater.inflate(R.layout.tag_form_editable, ll, false);
    			editableView1.setSaveEnabled(false);
    			editableView1.setTitle(preset);
    			ll.addView(editableView1, pos++);
    			nonEditable = addTagsToViews(editableView1, preset, (LinkedHashMap<String, String>) nonEditable);
    		}
    	} else {
    		nonEditable = allTags;
    	}
    	
    	LinearLayout nel = (LinearLayout) getView().findViewById(R.id.form_immutable_header_layout);
    	if (nel != null) {
    		nel.setVisibility(View.GONE);
    	}
    	if (nonEditable.size() > 0) {
    		nel.setVisibility(View.VISIBLE);
    		for (String key:nonEditable.keySet()) {
    			addRow(nonEditableView,key, nonEditable.get(key),null, allTags);
    		}
    	}
    	
    	if (focusOnAddress) {
    		focusOnAddress = false; // only do it once
    		if (!focusOnTag(Tags.KEY_ADDR_HOUSENUMBER)) {
    			if (!focusOnTag(Tags.KEY_ADDR_STREET)) {
    				focusOnEmpty();
    			}
    		} 
    	} else {
    		focusOnEmpty(); 
    	}
	}
	
	Map<String,String> addTagsToViews(LinearLayout editableView, PresetItem preset, LinkedHashMap<String, String> tags) {
		LinkedHashMap<String,String> recommendedEditable = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> optionalEditable = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> linkedTags = new LinkedHashMap<String,String>();
		LinkedHashMap<String,String> nonEditable = new LinkedHashMap<String,String>();
		HashMap<String,PresetItem> keyToLinkedPreset = new HashMap<String,PresetItem>();
		
		if (preset != null) {
			List<PresetItem> linkedPresets = preset.getLinkedPresets();
			for (String key:tags.keySet()) {
				if (preset.hasKeyValue(key, tags.get(key))) {
					if (preset.isFixedTag(key)) {
						// skip
					} else if (preset.isRecommendedTag(key)) {
						recommendedEditable.put(key, tags.get(key));
					} else {
						optionalEditable.put(key, tags.get(key));
					}
				} else {
					boolean found = false;
					if (linkedPresets != null) { // check if tag is in a linked preset
						for (PresetItem l:linkedPresets) {
							if (l.hasKeyValue(key, tags.get(key))) {
								linkedTags.put(key, tags.get(key));
								keyToLinkedPreset.put(key, l);
								found = true;
								break;
							}
						}
					}
					if (!found) {
						nonEditable.put(key, tags.get(key));
					}
				}
			}
		} else {
			Log.e(DEBUG_TAG,"addTagsToViews called with null preset");
		}
		for (String key:recommendedEditable.keySet()) {
			addRow(editableView,key, recommendedEditable.get(key),preset, tags);
		}
		for (String key:optionalEditable.keySet()) {
			addRow(editableView,key, optionalEditable.get(key),preset, tags);
		}
		for (String key: linkedTags.keySet()) {
			addRow(editableView,key, linkedTags.get(key), keyToLinkedPreset.get(key), tags);
		}

		return nonEditable;
	}
	
	private void addRow(LinearLayout rowLayout, final String key, final String value, PresetItem preset, LinkedHashMap<String, String> allTags) {
		if (rowLayout != null) {
			if (preset != null) {
				if (!preset.isFixedTag(key)) {
					ArrayAdapter<?> adapter = null;
					ArrayList<String> values = null;
					if (preset != null && preset.getKeyType(key) == PresetKeyType.MULTISELECT) {
						values = splitValues(Util.getArrayList(value), preset, key);
						adapter = getValueAutocompleteAdapter(key, values, preset, allTags);
					} else {
						adapter = getValueAutocompleteAdapter(key, Util.getArrayList(value), preset, allTags);
					}
					 
					int count = 0;
					if (adapter!=null) {
						count = adapter.getCount();
					} else {
						Log.d(DEBUG_TAG,"adapter null " + key + " " + value + " " + preset);
					}
					String hint = preset.getHint(key);
					//
					PresetKeyType keyType = preset.getKeyType(key);
					String defaultValue = preset.getDefault(key);
					
					if (keyType == PresetKeyType.TEXT 
						|| key.startsWith(Tags.KEY_ADDR_BASE)
						|| preset.isEditable(key)) {
						rowLayout.addView(addTextRow(rowLayout, preset, keyType, hint, key, value, defaultValue, adapter));
					} else if (preset.getKeyType(key) == PresetKeyType.COMBO || (keyType == PresetKeyType.CHECK && count > 2)) {
						if (count <= maxInlineValues) {
							rowLayout.addView(addComboRow(rowLayout, preset, hint, key, value, defaultValue, adapter));
						} else {
							rowLayout.addView(addComboDialogRow(rowLayout, preset, hint, key, value, defaultValue, adapter, allTags));
						}
					} else if (preset.getKeyType(key) == PresetKeyType.MULTISELECT) {
						if (count <= maxInlineValues) {
							rowLayout.addView(addMultiselectRow(rowLayout, preset, hint, key, values, defaultValue, adapter, allTags));
						} else {
							rowLayout.addView(addMultiselectDialogRow(rowLayout, preset, hint, key, value, defaultValue, adapter, allTags));
						}
					} else if (preset.getKeyType(key) == PresetKeyType.CHECK) {
						final TagCheckRow row = (TagCheckRow)inflater.inflate(R.layout.tag_form_check_row, rowLayout, false);
						row.keyView.setText(hint != null?hint:key);
						row.keyView.setTag(key);
						
						String v = "";
						String description = "";
						final String valueOn = preset.getOnValue(key);
						String tempValueOff = "";
						
						// this is a bit of a roundabout way of determining the non-checked value;
						for (int i=0;i< adapter.getCount();i++) {
							Object o = adapter.getItem(i);
							StringWithDescription swd = new StringWithDescription(o);
							v = swd.getValue();
							description = swd.getDescription();
							if (!v.equals(valueOn)) {
								tempValueOff = v;
							}
						}
						
						final String valueOff = tempValueOff;
						
						Log.d(DEBUG_TAG,"adapter size " + adapter.getCount() + " checked value >" + valueOn + "< not checked value >" + valueOff + "<");
						if (description==null) {
							description=v;
						}
						
						row.getCheckBox().setChecked(valueOn.equals(value));
						
						rowLayout.addView(row);
						row.getCheckBox().setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
							@Override
							public void onCheckedChanged(
									CompoundButton buttonView, boolean isChecked) {
								tagListener.updateSingleValue(key, isChecked?valueOn:valueOff);
							} 
						});
					} 
				}
//			} else if (key.startsWith(Tags.KEY_ADDR_BASE)) { // make address tags always editable
//				Set<String> usedKeys = allTags.keySet();
//				ArrayAdapter<?> adapter = null;
//				if (TagEditorFragment.isStreetName(key, usedKeys)) {
//					adapter = nameAdapters.getStreetNameAutocompleteAdapter(Util.getArrayList(value));
//				} else if (TagEditorFragment.isPlaceName(key, usedKeys)) {
//					adapter = nameAdapters.getPlaceNameAutocompleteAdapter(Util.getArrayList(value));
//				}
//				// String hint = preset.getHint(key);
//				rowLayout.addView(addTextRow(null, null, key, value, adapter));
			} else {
				if (false) { // make tags not associated with a preset un-editable, disabled for now, may end up in a preference
					final TagStaticTextRow row = (TagStaticTextRow)inflater.inflate(R.layout.tag_form_static_text_row, rowLayout, false);
					row.keyView.setText(key);
					row.valueView.setText(value);
					rowLayout.addView(row);
				} else {
					ArrayAdapter<?> adapter = getValueAutocompleteAdapter(key, Util.getArrayList(value), null, allTags);
					rowLayout.addView(addTextRow(rowLayout, null, PresetKeyType.TEXT, null, key, value, null, adapter));
				}
			}
		} else {
 			Log.d(DEBUG_TAG, "addRow rowLayout null");
 		}	
	}
	
	TagTextRow addTextRow(LinearLayout rowLayout, PresetItem preset, PresetKeyType keyType, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter) {
		final TagTextRow row = (TagTextRow)inflater.inflate(R.layout.tag_form_text_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.HONEYCOMB) { // stop Hint from wrapping
			row.valueView.setEllipsize(TruncateAt.END);
		}
		if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
			row.valueView.setText(defaultValue);
		} else {
			row.valueView.setText(value);
		}
		if (adapter != null) {
			row.valueView.setAdapter(adapter);
		} else {
			Log.e(DEBUG_TAG,"adapter null");
			row.valueView.setAdapter(new ArrayAdapter<String>(getActivity(), R.layout.autocomplete_row, new String[0]));
		}
		if (keyType==PresetKeyType.MULTISELECT) { 
			// FIXME this should be somewhere better obvious since it creates a non obvious side effect
			row.valueView.setTokenizer(new CustomAutoCompleteTextView.SingleCharTokenizer(preset.getDelimiter(key)));
		}
		if (keyType==PresetKeyType.TEXT && (adapter==null || adapter.getCount() < 2)) {
			row.valueView.setHint(R.string.tag_value_hint);
		} else {
			row.valueView.setHint(R.string.tag_autocomplete_value_hint);
		}
		OnClickListener autocompleteOnClick = new OnClickListener() {
			@Override
			public void onClick(View v) {
				if (v.hasFocus()) {
					Log.d(DEBUG_TAG,"onClick");
					((CustomAutoCompleteTextView)v).showDropDown();
				}
			}
		};
		row.valueView.setOnClickListener(autocompleteOnClick);
		row.valueView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
			@Override
			public void onFocusChange(View v, boolean hasFocus) {
				if (!hasFocus && !row.getValue().equals(value)) {
					Log.d(DEBUG_TAG,"onFocusChange");
					tagListener.updateSingleValue(key, row.getValue());
				}
			}
		});
		row.valueView.setOnItemClickListener(new OnItemClickListener() {
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
				Log.d(DEBUG_TAG,"onItemClicked value");
				Object o = parent.getItemAtPosition(position);
				if (o instanceof Names.NameAndTags) {
					row.valueView.setOrReplaceText(((NameAndTags)o).getName());
					// applyTagSuggestions(((NameAndTags)o).getTags());
				} else if (o instanceof ValueWithCount) {
					row.valueView.setOrReplaceText(((ValueWithCount)o).getValue());
				} else if (o instanceof StringWithDescription) {
					row.valueView.setOrReplaceText(((StringWithDescription)o).getValue());
				} else if (o instanceof String) {
					row.valueView.setOrReplaceText((String)o);
				}
				tagListener.updateSingleValue(key, row.getValue());
			}
		});
		
		return row;
	}
	
	TagComboRow addComboRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter) {
		final TagComboRow row = (TagComboRow)inflater.inflate(R.layout.tag_form_combo_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			String description = swd.getDescription();
			if (v==null || "".equals(v)) {
				continue;
			}
			if (description==null) {
				description=v;
			}
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
				row.addButton(description, v, v.equals(defaultValue));
			} else {
				row.addButton(description, v, v.equals(value));
			}
		}
		
		row.getRadioGroup().setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Log.d(DEBUG_TAG,"radio group onCheckedChanged");
				String value = "";
				if (checkedId != -1) {
					RadioButton button = (RadioButton) group.findViewById(checkedId);
					value = (String)button.getTag();	
				} 
				tagListener.updateSingleValue(key, value);
				row.setValue(value);
				row.setChanged(true);
			}
		});
		return row;
	}
	
	TagMultiselectRow addMultiselectRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final ArrayList<String> values, final String defaultValue, ArrayAdapter<?> adapter, LinkedHashMap<String, String> allTags) {
		final TagMultiselectRow row = (TagMultiselectRow)inflater.inflate(R.layout.tag_form_multiselect_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		row.setDelimiter(preset.getDelimiter(key));
		CompoundButton.OnCheckedChangeListener  onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(
					CompoundButton buttonView, boolean isChecked) {
				tagListener.updateSingleValue(key, row.getValue());
			} 
		};
		int count = adapter.getCount();
		for (int i=0;i<count;i++) {
			Object o = adapter.getItem(i);
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			String description = swd.getDescription();
			if (v==null || "".equals(v)) {
				continue;
			}
			if (description==null) {
				description=v;
			}

			if ((values==null || (values.size()==1 && "".equals(values.get(0)))) && (defaultValue != null && !"".equals(defaultValue))) {
				row.addCheck(description, v, v.equals(defaultValue), onCheckedChangeListener);
			} else {
				row.addCheck(description, v, values != null && values.contains(v), onCheckedChangeListener);
			}
		}
		return row;
	}
	
	TagFormDialogRow addComboDialogRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter, final LinkedHashMap<String, String> allTags) {
		final TagFormDialogRow row = (TagFormDialogRow)inflater.inflate(R.layout.tag_form_combo_dialog_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		row.setPreset(preset);
	
		String selectedValue=null;
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			String description = swd.getDescription();
			
			if (v==null || "".equals(v)) {
				continue;
			}
			if (description==null) {
				description=v;
			}
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue)) && v.equals(defaultValue)) {
				row.setValue(description,v);
				selectedValue = v;
				break;
			} else if (v.equals(value)){
				row.setValue(swd);
				selectedValue = v;
				break;
			}
		}
		row.valueView.setHint(R.string.tag_dialog_value_hint);
		final String finalSelectedValue;
		if (selectedValue != null) {
			finalSelectedValue = selectedValue;
		} else {
			finalSelectedValue = null;
		}
		row.setOnClickListener(new OnClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onClick(View v) {
				final View finalView = v;
				finalView.setEnabled(false); // debounce 
				final AlertDialog dialog = buildComboDialog(hint != null?hint:key,key,defaultValue,adapter,row); 
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
					dialog.setOnShowListener(new OnShowListener(){
						@Override
						public void onShow(DialogInterface d) {
							if (finalSelectedValue != null) {
								scrollDialogToValue(finalSelectedValue, dialog, R.id.valueGroup);
							}
						}});
				}
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						finalView.setEnabled(true);	
					}	
				});
				dialog.show();
			}
		});
		return row;
	}
	
	TagFormMultiselectDialogRow addMultiselectDialogRow(LinearLayout rowLayout, PresetItem preset, final String hint, final String key, final String value, final String defaultValue, final ArrayAdapter<?> adapter, final LinkedHashMap<String, String> allTags) {
		final TagFormMultiselectDialogRow row = (TagFormMultiselectDialogRow)inflater.inflate(R.layout.tag_form_multiselect_dialog_row, rowLayout, false);
		row.keyView.setText(hint != null?hint:key);
		row.keyView.setTag(key);
		row.setPreset(preset);
		Log.d(DEBUG_TAG, "addMultiselectDialogRow value " + value);
		ArrayList<String> multiselectValues = splitValues(Util.getArrayList(value),preset,key);
		
		ArrayList<StringWithDescription> selectedValues= new ArrayList<StringWithDescription>();
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			
			if (v==null || "".equals(v)) {
				continue;
			}
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue)) && v.equals(defaultValue)) {
				selectedValues.add(swd);
				break;
			} else if (multiselectValues != null) { 
				for (String m:multiselectValues) {
					if (v.equals(m)) {
						selectedValues.add(swd);
						break;
					}
				}
			}
		}
		row.setValue(selectedValues);
		row.valueView.setHint(R.string.tag_dialog_value_hint);
		
		row.setOnClickListener(new OnClickListener() {
			@SuppressLint("NewApi")
			@Override
			public void onClick(View v) {
				final View finalView = v;
				finalView.setEnabled(false); // debounce 
				final AlertDialog dialog =  buildMultiselectDialog(hint != null?hint:key,key,defaultValue,adapter,row, allTags);
				final Object tag = finalView.getTag();
				if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.FROYO) {
					dialog.setOnShowListener(new OnShowListener(){
						@Override
						public void onShow(DialogInterface d) {
							if (tag != null && tag instanceof String) {
								scrollDialogToValue((String)tag, dialog, R.id.valueGroup);
							}
						}});
				}
				dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
					@Override
					public void onDismiss(DialogInterface dialog) {
						finalView.setEnabled(true);	
					}	
				});
				dialog.show();
				// the following is one big awful hack to stop the button dismissing the dialog
				dialog.getButton(AlertDialog.BUTTON_NEUTRAL).setOnClickListener(new View.OnClickListener() {            
					@Override
					public void onClick(View v)
					{
						LinearLayout valueGroup = (LinearLayout) dialog.findViewById(R.id.valueGroup);
						for (int pos=0;pos < valueGroup.getChildCount();pos++) {
							View c = valueGroup.getChildAt(pos);
							if (c != null && c instanceof AppCompatCheckBox) {
								((AppCompatCheckBox)c).setChecked(false);
							}
						}
					}
				});

			}
		});
		return row;
	}

	/**
	 * Build a dialog for selecting a single value of none via a scrollable list of radio buttons
	 * @param hint
	 * @param key
	 * @param defaultValue
	 * @param adapter
	 * @param row
	 * @return
	 */
	protected AlertDialog buildComboDialog(String hint,String key,String defaultValue,final ArrayAdapter<?> adapter,final TagFormDialogRow row) {
		String value = row.getValue();
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(hint);
	   	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	
		final View layout = inflater.inflate(R.layout.form_combo_dialog, null);
		RadioGroup valueGroup = (RadioGroup) layout.findViewById(R.id.valueGroup);
		builder.setView(layout);
		
		View.OnClickListener listener = new View.OnClickListener(){
			@Override
			public void onClick(View v) {
				Log.d(DEBUG_TAG,"radio button clicked " + row.getValue() + " " + v.getTag());
				if (!row.hasChanged()) {
					RadioGroup g = (RadioGroup) v.getParent();
					g.clearCheck();
				} else {
					row.setChanged(false);
				}
			}
		};
		
		LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
		buttonLayoutParams.width = LayoutParams.FILL_PARENT;
		
		for (int i=0;i< adapter.getCount();i++) {
			Object o = adapter.getItem(i);
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			
			if (v==null || "".equals(v)) {
				continue;
			}
			
			if ((value == null || "".equals(value)) && (defaultValue != null && !"".equals(defaultValue))) {
				addButton(getActivity(), valueGroup, i, swd, v.equals(defaultValue), listener, buttonLayoutParams);
			} else {			
				addButton(getActivity(), valueGroup, i, swd, v.equals(value), listener, buttonLayoutParams);
			}
		}
		final Handler handler = new Handler(); 
		builder.setPositiveButton(R.string.clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				tagListener.updateSingleValue((String) layout.getTag(), "");
				row.setValue("","");
				row.setChanged(true);
				final DialogInterface finalDialog = dialog;
				// allow a tiny bit of time to see that the action actually worked
				handler.postDelayed(new Runnable(){@Override public void run() {finalDialog.dismiss();}}, 100);	
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		final AlertDialog dialog = builder.create();
		layout.setTag(key);
		valueGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {
				Log.d(DEBUG_TAG,"radio group onCheckedChanged");
				StringWithDescription value = null;
				if (checkedId != -1) {
					RadioButton button = (RadioButton) group.findViewById(checkedId);
					value = (StringWithDescription)button.getTag();	
					tagListener.updateSingleValue((String) layout.getTag(), value.getValue());
					row.setValue(value);
					row.setChanged(true);
				}
				// allow a tiny bit of time to see that the action actually worked
				handler.postDelayed(new Runnable(){@Override public void run() {dialog.dismiss();}}, 100);
			}
		});
		
		return dialog;
	}
	
	protected AlertDialog buildMultiselectDialog(String hint,String key,String defaultValue,ArrayAdapter<?> adapter,final TagFormMultiselectDialogRow row, LinkedHashMap<String, String> allTags) {	
		Builder builder = new AlertDialog.Builder(getActivity());
		builder.setTitle(hint);
	   	final LayoutInflater inflater = ThemeUtils.getLayoutInflater(getActivity());
    	
		final View layout = inflater.inflate(R.layout.form_multiselect_dialog, null);
		final LinearLayout valueGroup = (LinearLayout) layout.findViewById(R.id.valueGroup);
		builder.setView(layout);
		
		LayoutParams buttonLayoutParams = valueGroup.getLayoutParams();
		buttonLayoutParams.width = LayoutParams.FILL_PARENT;
		
		layout.setTag(key);
		ArrayList<String> values = splitValues(Util.getArrayList(row.getValue()), row.getPreset(), key);
		
		int count = adapter.getCount();
		for (int i=0;i<count;i++) {
			Object o = adapter.getItem(i);
			StringWithDescription swd = new StringWithDescription(o);
			String v = swd.getValue();
			String description = swd.getDescription();
			if (v==null || "".equals(v)) {
				continue;
			}
			if (description==null) {
				description=v;
			}

			if ((values==null || (values.size()==1 && "".equals(values.get(0)))) && (defaultValue != null && !"".equals(defaultValue))) {
				addCheck(getActivity(), valueGroup, swd, v.equals(defaultValue), buttonLayoutParams);
			} else {			
				addCheck(getActivity(), valueGroup, swd, values.contains(v), buttonLayoutParams);
			}
		}
		
		builder.setNeutralButton(R.string.clear, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
			}
		});
		builder.setPositiveButton(R.string.save, new DialogInterface.OnClickListener() {
			@Override
			public void onClick(DialogInterface dialog, int which) {
				ArrayList<StringWithDescription> values = new ArrayList<StringWithDescription>();
				for (int pos=0;pos<valueGroup.getChildCount();pos++) {
					View c = valueGroup.getChildAt(pos);
					if (c != null && c instanceof AppCompatCheckBox) {
						AppCompatCheckBox checkBox = (AppCompatCheckBox)c;
						if (checkBox.isChecked()) {
							values.add((StringWithDescription) checkBox.getTag());
						}
					}
				}
				row.setValue(values);
				tagListener.updateSingleValue((String) layout.getTag(), row.getValue());
				row.setChanged(true);
			}
		});
		builder.setNegativeButton(R.string.cancel, null);
		final AlertDialog dialog = builder.create();
		return dialog;
	}

	/**
	 * Scroll the view in the dialog to show the value, assumes the ScrollView has id R.is.scrollView
	 * @param key
	 * @return
	 */
	private void scrollDialogToValue(String value, AlertDialog dialog, int containerId) {
		Log.d(DEBUG_TAG,"scrollDialogToValue scrolling to " + value);
		final View sv = (View) dialog.findViewById(R.id.myScrollView);
		if (sv != null) {
			ViewGroup container = (ViewGroup) dialog.findViewById(containerId);
			if (container != null) {
				for (int pos = 0;pos < container.getChildCount();pos++) {
					View child = container.getChildAt(pos);
					Object tag = child.getTag();
					if (tag != null && tag instanceof StringWithDescription && ((StringWithDescription)tag).equals(value)) {
						final View finalChild = child;
						Util.scrollToRow(sv, finalChild, true, true);
						return;
					}
				}
			} else {
				Log.d(DEBUG_TAG,"scrollDialogToValue container view null");
			}	
		} else {
			Log.d(DEBUG_TAG,"scrollDialogToValue scroll view null");
		}	
	}
	
	/**
	 * Focus on the value field of a tag with key "key" 
	 * @param key
	 * @return
	 */
	private boolean focusOnTag(String key) {
		boolean found = false;
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			int pos = 0;
			while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
				EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
				for (int i = ll2.getChildCount() - 1; i >= 0; --i) {
					View v = ll2.getChildAt(i);
					if (v instanceof TagTextRow && ((TagTextRow)v).getKey().equals(key)) {
						((TagTextRow)v).getValueView().requestFocus();
						Util.scrollToRow(sv, v, true, true);
						found = true;
						break;
					}
				}
				pos++;
			}
		} else {
			Log.d(DEBUG_TAG,"focusOnTag container layout null");
			return false;
		}	
		return found;
	}
	
	/**
	 * Focus on the first empty value field 
	 * @return
	 */
	private boolean focusOnEmpty() {
		boolean found = false;
		View sv = getView();
		LinearLayout ll = (LinearLayout) sv.findViewById(R.id.form_container_layout);
		if (ll != null) {
			int pos = 0;
			while (ll.getChildAt(pos) instanceof EditableLayout && pos < ll.getChildCount() && !found) {
				EditableLayout ll2 = (EditableLayout) ll.getChildAt(pos);
				for (int i = 0 ; i < ll2.getChildCount(); i++) {
					View v = ll2.getChildAt(i);
					if (v instanceof TagTextRow && "".equals(((TagTextRow)v).getValue())) {
						((TagTextRow)v).getValueView().requestFocus();
						found = true;
						break;
					}
				}
				pos++;
			}
		} else {
			Log.d(DEBUG_TAG,"update container layout null");
			return false;
		}	
		return found;
	}
	
	public static class TagTextRow extends LinearLayout {

		private TextView keyView;
		private CustomAutoCompleteTextView valueView;
		
		public TagTextRow(Context context) {
			super(context);
		}
		
		public TagTextRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (CustomAutoCompleteTextView)findViewById(R.id.textValue);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public String getValue() { 
			return valueView.getText().toString();
		}
		
		public CustomAutoCompleteTextView getValueView() {
			return valueView;
		}
	}
	
	public static class TagStaticTextRow extends LinearLayout {

		private TextView keyView;
		private TextView valueView;
		
		public TagStaticTextRow(Context context) {
			super(context);
		}
		
		public TagStaticTextRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}		

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (TextView)findViewById(R.id.textValue);
		}	
	}
	
	public static class TagComboRow extends LinearLayout {

		private TextView keyView;
		private RadioGroup valueGroup;
		private String value;
		private Context context;
		private int idCounter = 0;
		private boolean changed = false;
		
		public TagComboRow(Context context) {
			super(context);
			this.context = context;
		}
		
		public TagComboRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			this.context = context;
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueGroup = (RadioGroup)findViewById(R.id.valueGroup);
			
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public RadioGroup getRadioGroup() { 
			return valueGroup;
		}
		
		public void setValue(String value) {
			this.value = value;
		}

		public String getValue() {
			return value;
		}
		
		public void setChanged(boolean changed) {
			this.changed = changed;
		}
		
		public boolean hasChanged() {
			return changed;
		}
		
		public void addButton(String description, String value, boolean selected) {
			final AppCompatRadioButton button = new AppCompatRadioButton(context);
			button.setText(description);
			button.setTag(value);
			button.setChecked(selected);
			button.setId(idCounter++);
			valueGroup.addView(button);
			if (selected) {
				setValue(value);
			}
			button.setOnClickListener(new OnClickListener() {
				@Override
				public void onClick(View v) {
					Log.d(DEBUG_TAG,"radio button clicked " + getValue() + " " + button.getTag());
					if (!changed) {
						RadioGroup g = (RadioGroup) v.getParent();
						g.clearCheck();
					} else {
						changed = false;
					}
				}
			});
		}
	}
	
	public void addButton(Context context, RadioGroup group, int id, StringWithDescription swd, boolean selected, View.OnClickListener listener, LayoutParams layoutParams) {
		final AppCompatRadioButton button = new AppCompatRadioButton(context);
		String description = swd.getDescription();
		button.setText(description != null && !"".equals(description)?description:swd.getValue());
		button.setTag(swd);
		button.setChecked(selected);
		button.setId(id);
		button.setLayoutParams(layoutParams);
		group.addView(button);
		button.setOnClickListener(listener);
	}
	
	/**
	 * Display a single value and allow editing via a dialog
	 */
	public static class TagFormDialogRow extends LinearLayout {

		protected TextView keyView;
		protected TextView valueView;
		private String value;
		private boolean changed = false;
		protected PresetItem preset;
		
		public TagFormDialogRow(Context context) {
			super(context);
		}

		public TagFormDialogRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueView = (TextView)findViewById(R.id.textValue);	
		}
		
		public void setOnClickListener(final OnClickListener listener) {
			valueView.setOnClickListener(listener);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public void setValue(String value, String description) {
			this.value = value;
			valueView.setText(description);
			valueView.setTag(value);
		}
		
		public void setValue(StringWithDescription swd) {
			String description = swd.getDescription();
			setValue(swd.getValue(),description != null && !"".equals(description)?description:swd.getValue());
		}

		public String getValue() {
			return value;
		}
		
		public void setChanged(boolean changed) {
			this.changed = changed;
		}
		
		public boolean hasChanged() {
			return changed;
		}
				
		public void setPreset(PresetItem preset) {
			this.preset = preset;
		}
	
		public PresetItem getPreset() {
			return preset;
		}
	}
	
	/**
	 * Row that displays multiselect values and allows changing them via a dialog
	 */
	public static class TagFormMultiselectDialogRow extends TagFormDialogRow {
		
		OnClickListener listener;

		LinearLayout valueList;
		final LayoutInflater inflater;
		
		public TagFormMultiselectDialogRow(Context context) {
			super(context);
			inflater = LayoutInflater.from(context);
		}
		
		public TagFormMultiselectDialogRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			inflater = LayoutInflater.from(context);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			valueList = (LinearLayout)findViewById(R.id.valueList);
		}
		
		/**
		 * Set the onclicklistener for every value
		 */
		public void setOnClickListener(final OnClickListener listener) {
			this.listener = listener;
			for (int pos=0;pos<valueList.getChildCount();pos++) {
				View v = valueList.getChildAt(pos);
				if (v instanceof TextView) {
					((TextView)v).setOnClickListener(listener);
				}
			}
		}
		
		/**
		 * Add additional description values as individual TextViews
		 * @param values
		 */
		public void setValue(ArrayList<StringWithDescription> values) {
			String value = "";
			char delimiter = preset.getDelimiter(getKey());
			int childCont = valueList.getChildCount();
			for (int pos = 1;pos < childCont ;pos++) { // don^t delete first child
				valueList.removeViewAt(1);
			}
			boolean first=true;
			for (StringWithDescription swd:values) {
				String d = swd.getDescription();
				if (first) {
					setValue(swd.getValue(),d != null && !"".equals(d)?d:swd.getValue());
					first = false;
				} else {
					TextView extraValue = (TextView)inflater.inflate(R.layout.form_dialog_multiselect_value, valueList, false);
					extraValue.setText(d != null && !"".equals(d)?d:swd.getValue());
					extraValue.setTag(swd.getValue());
					valueList.addView(extraValue);
				}
				// collect the individual values for what we actually store
				if ("".equals(value)) {
					value = swd.getValue();
				} else {
					value = value + delimiter + swd.getValue();
				}
			}
			super.value = value;
			setOnClickListener(listener);
		}
	}
	
	/**
	 * Inline multiselect value display with checkboxes
	 */
	public static class TagMultiselectRow extends LinearLayout {
		private TextView keyView;
		private LinearLayout valueLayout;
		private Context context;
		private char delimiter;
		
		public TagMultiselectRow(Context context) {
			super(context);
			this.context = context;
		}
		
		public TagMultiselectRow(Context context, AttributeSet attrs) {
			super(context, attrs);
			this.context = context;
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueLayout = (LinearLayout)findViewById(R.id.valueGroup);
			
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public LinearLayout getValueGroup() { 
			return valueLayout;
		}
		
		/**
		 * Return all checked values concatenated with the required delimiter
		 * @return
		 */
		public String getValue() {
			StringBuilder result = new StringBuilder();
			for (int i=0;i<valueLayout.getChildCount();i++) {
				AppCompatCheckBox check = (AppCompatCheckBox) valueLayout.getChildAt(i);
				if (check.isChecked()) {
					if (result.length() > 0) { // not the first entry
						result.append(delimiter);
					}
					result.append(valueLayout.getChildAt(i).getTag());
				}
			}
			return result.toString();
		}
		
		public void setDelimiter(char delimiter) {
			this.delimiter = delimiter;
		}
		
		public void addCheck(String description, String value, boolean selected, CompoundButton.OnCheckedChangeListener listener) {
			final AppCompatCheckBox check = new AppCompatCheckBox(context);
			check.setText(description);
			check.setTag(value);
			check.setChecked(selected);
			valueLayout.addView(check);
			check.setOnCheckedChangeListener(listener);
		}
	}
	
	public void addCheck(Context context, LinearLayout layout, StringWithDescription swd, boolean selected, LayoutParams layoutParams) {
		final AppCompatCheckBox check = new AppCompatCheckBox(context);
		String description = swd.getDescription();
		check.setText(description != null && !"".equals(description)?description:swd.getValue());
		check.setTag(swd);
		check.setLayoutParams(layoutParams);
		check.setChecked(selected);
		layout.addView(check);
	}
	
	public static class TagCheckRow extends LinearLayout {

		private TextView keyView;
		private AppCompatCheckBox valueCheck;
		
		public TagCheckRow(Context context) {
			super(context);
		}
		
		public TagCheckRow(Context context, AttributeSet attrs) {
			super(context, attrs);
		}
		
		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			keyView = (TextView)findViewById(R.id.textKey);
			valueCheck = (AppCompatCheckBox)findViewById(R.id.valueSelected);
		}
		
		/**
		 * Return the OSM key value
		 * @return
		 */
		public String getKey() {
			return (String) keyView.getTag();
		}
		
		public AppCompatCheckBox getCheckBox() {
			return valueCheck;
		}
		
		public boolean isChecked() { 
			return valueCheck.isChecked();
		}
	}
	
	public static class EditableLayout extends LinearLayout {

		private ImageView headerIconView;
		private TextView headerTitleView;
		private LinearLayout rowLayout;
		
		public  EditableLayout(Context context) {
			super(context);
		}
		
		public  EditableLayout(Context context, AttributeSet attrs) {
			super(context, attrs);
		}		

		@Override
		protected void onFinishInflate() {
			super.onFinishInflate();
			if (isInEditMode()) return; // allow visual editor to work
			
			headerIconView = (ImageView)findViewById(R.id.form_header_icon_view);
			headerTitleView = (TextView)findViewById(R.id.form_header_title);
			rowLayout = (LinearLayout) findViewById(R.id.form_editable_row_layout);
		}	
		
		private void setMyVisibility(int visibility) {
			rowLayout.setVisibility(visibility);
			for (int i=0;i < rowLayout.getChildCount();i++) {
				rowLayout.getChildAt(i).setVisibility(visibility);
			}
		}
		
		public void close() {
			Log.d(DEBUG_TAG,"close");
			setMyVisibility(View.GONE);
		}
		
		public void open() {
			Log.d(DEBUG_TAG,"open");
			setMyVisibility(View.VISIBLE);
		}
		
		public void setTitle(PresetItem preset) {

			if (preset != null) {
				Drawable icon = preset.getIcon();
				if (icon != null) {
					headerIconView.setVisibility(View.VISIBLE);
					//NOTE directly using the icon seems to trash it, so make a copy
					headerIconView.setImageDrawable(icon.getConstantState().newDrawable()); 
				} else {
					headerIconView.setVisibility(View.GONE);
				}
				headerTitleView.setText(preset.getTranslatedName());
			} else {
				headerTitleView.setText("Unknown element (no preset)");
			}
		}
	}

	@Override
	public void tagsUpdated() {
		update();	
	}
}