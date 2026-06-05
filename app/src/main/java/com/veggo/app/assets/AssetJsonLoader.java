package com.veggo.app.assets;

import android.content.Context;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.reflect.TypeToken;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.lang.reflect.Type;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

public final class AssetJsonLoader {
    private final Context context;
    private final Gson gson;

    public AssetJsonLoader(Context context) {
        this(context, new Gson());
    }

    public AssetJsonLoader(Context context, Gson gson) {
        this.context = context.getApplicationContext();
        this.gson = gson;
    }

    public <T> List<T> readList(String fileName, Class<T> itemClass) throws IOException {
        Type type = TypeToken.getParameterized(List.class, itemClass).getType();
        return read(fileName, type);
    }

    public <T> T readObject(String fileName, Class<T> objectClass) throws IOException {
        return read(fileName, objectClass);
    }

    public JsonElement readJson(String fileName) throws IOException {
        return read(fileName, JsonElement.class);
    }

    public Map<String, AssetModels.LocationNode> readLocationTree() throws IOException {
        Type type = new TypeToken<Map<String, AssetModels.LocationNode>>() {
        }.getType();
        return read(AssetFiles.TREE_COMPLETES, type);
    }

    private <T> T read(String fileName, Type type) throws IOException {
        try (InputStream inputStream = context.getAssets().open(fileName);
             InputStreamReader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8)) {
            return gson.fromJson(reader, type);
        }
    }
}
