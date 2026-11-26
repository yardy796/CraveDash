package com.example.cravedash;

import android.app.AlertDialog;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private DatabaseReference databaseReference;
    private List<FoodItem> allFoodList;
    private FoodAdapter adapter;
    private EditText etSearchFood;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseReference = FirebaseDatabase.getInstance().getReference("foods");
        allFoodList = new ArrayList<>();

        etSearchFood = findViewById(R.id.etSearchFood);
        RecyclerView recyclerView = findViewById(R.id.recyclerFood);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new FoodAdapter(new ArrayList<>(), new FoodAdapter.OnFoodActionListener() {
            @Override
            public void onEdit(FoodItem item) {
                showFoodDialog(item);
            }

            @Override
            public void onDelete(FoodItem item) {
                databaseReference.child(item.getId()).removeValue()
                        .addOnSuccessListener(unused -> Toast.makeText(MainActivity.this, "Dish removed", Toast.LENGTH_SHORT).show());
            }
        });
        recyclerView.setAdapter(adapter);

        ExtendedFloatingActionButton fabAddFood = findViewById(R.id.fabAddFood);
        fabAddFood.setOnClickListener(v -> showFoodDialog(null));

        etSearchFood.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterFoods(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {
            }
        });

        readFoodItems();
    }

    private void filterFoods(String query) {
        String lowerQuery = query.trim().toLowerCase();
        List<FoodItem> filtered = new ArrayList<>();
        for (FoodItem item : allFoodList) {
            boolean matchesName = item.getName() != null && item.getName().toLowerCase().contains(lowerQuery);
            boolean matchesCategory = item.getCategory() != null && item.getCategory().toLowerCase().contains(lowerQuery);
            if (lowerQuery.isEmpty() || matchesName || matchesCategory) {
                filtered.add(item);
            }
        }
        adapter.updateList(filtered);
    }

    private void readFoodItems() {
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                allFoodList.clear();
                for (DataSnapshot data : snapshot.getChildren()) {
                    FoodItem item = data.getValue(FoodItem.class);
                    if (item != null) {
                        allFoodList.add(item);
                    }
                }
                filterFoods(etSearchFood.getText().toString());
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(MainActivity.this, error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showFoodDialog(FoodItem existingItem) {
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_food_form, null);
        EditText etFoodName = view.findViewById(R.id.etFoodName);
        EditText etCategory = view.findViewById(R.id.etCategory);
        EditText etPrice = view.findViewById(R.id.etPrice);

        if (existingItem != null) {
            etFoodName.setText(existingItem.getName());
            etCategory.setText(existingItem.getCategory());
            etPrice.setText(existingItem.getPrice());
        }

        new AlertDialog.Builder(this)
                .setTitle(existingItem == null ? "Add New Dish" : "Update Dish")
                .setView(view)
                .setPositiveButton(existingItem == null ? "Create" : "Update", (dialog, which) -> {
                    String name = etFoodName.getText().toString().trim();
                    String category = etCategory.getText().toString().trim();
                    String price = etPrice.getText().toString().trim();

                    if (name.isEmpty() || category.isEmpty() || price.isEmpty()) {
                        Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    if (existingItem == null) {
                        String id = databaseReference.push().getKey();
                        if (id != null) {
                            FoodItem newItem = new FoodItem(id, name, category, price);
                            databaseReference.child(id).setValue(newItem);
                        }
                    } else {
                        FoodItem updatedItem = new FoodItem(existingItem.getId(), name, category, price);
                        databaseReference.child(existingItem.getId()).setValue(updatedItem);
                    }
                })
                .setNegativeButton("Cancel", null)
                .show();
    }
}