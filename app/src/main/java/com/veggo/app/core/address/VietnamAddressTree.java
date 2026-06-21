package com.veggo.app.core.address;

import androidx.annotation.Nullable;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import java.text.Collator;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public final class VietnamAddressTree {
    private final Map<String, Map<String, List<String>>> provinceToDistricts = new LinkedHashMap<>();
    private final Collator vietnameseCollator = Collator.getInstance(new Locale("vi", "VN"));

    private VietnamAddressTree() {
        vietnameseCollator.setStrength(Collator.PRIMARY);
    }

    public static VietnamAddressTree parse(JsonArray rootArray) {
        VietnamAddressTree tree = new VietnamAddressTree();
        if (rootArray == null || rootArray.size() == 0) {
            return tree;
        }

        JsonElement rootElement = rootArray.get(0);
        if (!rootElement.isJsonObject()) {
            return tree;
        }

        JsonObject provincesObject = rootElement.getAsJsonObject();
        for (Map.Entry<String, JsonElement> provinceEntry : provincesObject.entrySet()) {
            String entryKey = provinceEntry.getKey();
            if ("_id".equals(entryKey) || "__v".equals(entryKey)) {
                continue;
            }
            if (!provinceEntry.getValue().isJsonObject()) {
                continue;
            }
            JsonObject province = provinceEntry.getValue().getAsJsonObject();
            String provinceName = readName(province);
            if (provinceName.isEmpty()) {
                continue;
            }

            Map<String, List<String>> districts = new LinkedHashMap<>();
            JsonElement districtContainer = province.get("quan-huyen");
            if (districtContainer != null && districtContainer.isJsonObject()) {
                for (Map.Entry<String, JsonElement> districtEntry : districtContainer.getAsJsonObject().entrySet()) {
                    if (!districtEntry.getValue().isJsonObject()) {
                        continue;
                    }
                    JsonObject district = districtEntry.getValue().getAsJsonObject();
                    String districtName = readName(district);
                    if (districtName.isEmpty()) {
                        continue;
                    }

                    List<String> wards = new ArrayList<>();
                    JsonElement wardContainer = district.get("xa-phuong");
                    if (wardContainer != null && wardContainer.isJsonObject()) {
                        for (Map.Entry<String, JsonElement> wardEntry : wardContainer.getAsJsonObject().entrySet()) {
                            if (!wardEntry.getValue().isJsonObject()) {
                                continue;
                            }
                            String wardName = readName(wardEntry.getValue().getAsJsonObject());
                            if (!wardName.isEmpty()) {
                                wards.add(wardName);
                            }
                        }
                    }
                    tree.sortNames(wards);
                    districts.put(districtName, wards);
                }
            }

            List<String> districtNames = new ArrayList<>(districts.keySet());
            tree.sortNames(districtNames);
            Map<String, List<String>> sortedDistricts = new LinkedHashMap<>();
            for (String districtName : districtNames) {
                sortedDistricts.put(districtName, districts.get(districtName));
            }
            tree.provinceToDistricts.put(provinceName, sortedDistricts);
        }

        return tree;
    }

    public boolean isEmpty() {
        return provinceToDistricts.isEmpty();
    }

    public List<String> getProvinces() {
        List<String> provinces = new ArrayList<>(provinceToDistricts.keySet());
        sortNames(provinces);
        return provinces;
    }

    public List<String> getDistricts(String province) {
        Map<String, List<String>> districts = findDistrictMap(province);
        if (districts == null) {
            return Collections.emptyList();
        }
        return new ArrayList<>(districts.keySet());
    }

    public List<String> getWards(String province, String district) {
        Map<String, List<String>> districts = findDistrictMap(province);
        if (districts == null) {
            return Collections.emptyList();
        }
        List<String> wards = districts.get(district);
        if (wards == null) {
            for (Map.Entry<String, List<String>> entry : districts.entrySet()) {
                if (namesMatch(entry.getKey(), district)) {
                    return new ArrayList<>(entry.getValue());
                }
            }
            return Collections.emptyList();
        }
        return new ArrayList<>(wards);
    }

    @Nullable
    public String findMatchingProvince(String value) {
        return findMatchingName(getProvinces(), value);
    }

    @Nullable
    public String findMatchingDistrict(String province, String value) {
        return findMatchingName(getDistricts(province), value);
    }

    @Nullable
    public String findMatchingWard(String province, String district, String value) {
        return findMatchingName(getWards(province, district), value);
    }

    @Nullable
    private Map<String, List<String>> findDistrictMap(String province) {
        Map<String, List<String>> districts = provinceToDistricts.get(province);
        if (districts != null) {
            return districts;
        }
        for (Map.Entry<String, Map<String, List<String>>> entry : provinceToDistricts.entrySet()) {
            if (namesMatch(entry.getKey(), province)) {
                return entry.getValue();
            }
        }
        return null;
    }

    @Nullable
    private String findMatchingName(List<String> options, @Nullable String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        for (String option : options) {
            if (namesMatch(option, value)) {
                return option;
            }
        }
        return null;
    }

    private boolean namesMatch(String left, String right) {
        if (left == null || right == null) {
            return false;
        }
        String normalizedLeft = normalizeName(left);
        String normalizedRight = normalizeName(right);
        return normalizedLeft.equals(normalizedRight)
                || normalizedLeft.contains(normalizedRight)
                || normalizedRight.contains(normalizedLeft);
    }

    private String normalizeName(String value) {
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private void sortNames(List<String> names) {
        Collections.sort(names, vietnameseCollator);
    }

    private static String readName(JsonObject node) {
        if (node.has("name") && !node.get("name").isJsonNull()) {
            return node.get("name").getAsString().trim();
        }
        if (node.has("name_with_type") && !node.get("name_with_type").isJsonNull()) {
            return node.get("name_with_type").getAsString().trim();
        }
        return "";
    }
}
