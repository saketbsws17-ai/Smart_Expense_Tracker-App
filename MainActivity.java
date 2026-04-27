package com.example.smartexpensetracker;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.smartexpensetracker.adapter.CategorySummaryAdapter;
import com.example.smartexpensetracker.adapter.ExpenseAdapter;
import com.example.smartexpensetracker.data.ExpenseDatabase;
import com.example.smartexpensetracker.databinding.ActivityMainBinding;
import com.example.smartexpensetracker.model.CategoryTotal;
import com.example.smartexpensetracker.model.Expense;
import com.example.smartexpensetracker.util.AnalysisEngine;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.snackbar.Snackbar;
import com.google.android.material.textfield.MaterialAutoCompleteTextView;

import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int SMS_PERMISSION_REQUEST_CODE = 1001;

    private final List<Expense> expenseList = new ArrayList<>();
    private final NumberFormat currencyFormat =
            NumberFormat.getCurrencyInstance(new Locale("en", "IN"));
    private final DateTimeFormatter dateFormatter =
            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH);

    private ActivityMainBinding binding;
    private ExpenseAdapter expenseAdapter;
    private CategorySummaryAdapter categorySummaryAdapter;
    private ExpenseDatabase expenseDatabase;
    private LocalDate selectedDate = LocalDate.now();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        expenseDatabase = ExpenseDatabase.getInstance(this);

        setupDropdowns();
        setupRecyclerViews();
        setupDatePicker();
        requestSmsPermissionsIfNeeded();
        loadExpensesFromDatabase();

        binding.addExpenseButton.setOnClickListener(view -> addExpense());
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadExpensesFromDatabase();
    }

    private void setupDropdowns() {
        bindDropdown(binding.categorySpinner, R.array.categories);
        bindDropdown(binding.moodSpinner, R.array.moods);
        bindDropdown(binding.necessitySpinner, R.array.necessity_levels);
        bindDropdown(binding.paymentSpinner, R.array.payment_methods);
    }

    private void bindDropdown(MaterialAutoCompleteTextView dropdown, int arrayRes) {
        String[] options = getResources().getStringArray(arrayRes);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(
                this,
                android.R.layout.simple_list_item_1,
                options
        );
        dropdown.setAdapter(adapter);

        if (options.length > 0) {
            dropdown.setText(options[0], false);
        }
    }

    private void setupRecyclerViews() {
        expenseAdapter = new ExpenseAdapter();
        categorySummaryAdapter = new CategorySummaryAdapter();

        binding.expensesRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.expensesRecycler.setAdapter(expenseAdapter);
        binding.expensesRecycler.setNestedScrollingEnabled(false);

        binding.categoryRecycler.setLayoutManager(new LinearLayoutManager(this));
        binding.categoryRecycler.setAdapter(categorySummaryAdapter);
        binding.categoryRecycler.setNestedScrollingEnabled(false);
    }

    private void setupDatePicker() {
        updateDateButton();

        binding.dateButton.setOnClickListener(view -> {
            DatePickerDialog dialog = new DatePickerDialog(
                    this,
                    (picker, year, month, dayOfMonth) -> {
                        selectedDate = LocalDate.of(year, month + 1, dayOfMonth);
                        updateDateButton();
                    },
                    selectedDate.getYear(),
                    selectedDate.getMonthValue() - 1,
                    selectedDate.getDayOfMonth()
            );
            dialog.show();
        });
    }

    private void updateDateButton() {
        binding.dateButton.setText(selectedDate.format(dateFormatter));
    }

    private void addExpense() {
        clearErrors();

        String amountText = valueOf(binding.amountInput.getText());
        String reason = valueOf(binding.reasonInput.getText());
        String category = valueOf(binding.categorySpinner.getText());
        String mood = valueOf(binding.moodSpinner.getText());
        String necessityText = valueOf(binding.necessitySpinner.getText());
        String paymentMethod = valueOf(binding.paymentSpinner.getText());

        if (amountText.isEmpty()) {
            binding.amountLayout.setError(getString(R.string.error_amount_required));
            return;
        }

        if (reason.isEmpty()) {
            binding.reasonLayout.setError(getString(R.string.error_reason_required));
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException exception) {
            binding.amountLayout.setError(getString(R.string.error_amount_invalid));
            return;
        }

        if (amount <= 0) {
            binding.amountLayout.setError(getString(R.string.error_amount_invalid));
            return;
        }

        int necessityLevel = Integer.parseInt(necessityText);

        Expense expense = new Expense(
                amount,
                category,
                selectedDate.format(dateFormatter),
                paymentMethod,
                reason,
                necessityLevel,
                mood,
                System.currentTimeMillis(),
                "MANUAL"
        );

        expenseDatabase.expenseDao().insert(expense);
        expenseList.add(0, expense);
        refreshDashboard();
        showAddedDialog(expense);
        resetForm();
    }

    private void showAddedDialog(Expense expense) {
        String message =
                getString(R.string.dialog_amount, currencyFormat.format(expense.getAmount())) + "\n" +
                getString(R.string.dialog_category, expense.getCategory()) + "\n" +
                getString(R.string.dialog_type, AnalysisEngine.detectReasonType(expense)) + "\n" +
                getString(R.string.dialog_score, Math.round(AnalysisEngine.financialScore(expenseList)));

        new MaterialAlertDialogBuilder(this)
                .setTitle(R.string.expense_added_title)
                .setMessage(message)
                .setPositiveButton(android.R.string.ok, null)
                .show();
    }

    private void resetForm() {
        binding.amountInput.setText("");
        binding.reasonInput.setText("");
        resetDropdown(binding.categorySpinner, R.array.categories);
        resetDropdown(binding.moodSpinner, R.array.moods);
        resetDropdown(binding.necessitySpinner, R.array.necessity_levels);
        resetDropdown(binding.paymentSpinner, R.array.payment_methods);
        selectedDate = LocalDate.now();
        updateDateButton();
        Snackbar.make(binding.getRoot(), R.string.expense_added_snackbar, Snackbar.LENGTH_SHORT).show();
    }

    private void clearErrors() {
        binding.amountLayout.setError(null);
        binding.reasonLayout.setError(null);
    }

    private void resetDropdown(MaterialAutoCompleteTextView dropdown, int arrayRes) {
        String[] options = getResources().getStringArray(arrayRes);
        if (options.length > 0) {
            dropdown.setText(options[0], false);
        }
    }

    private void loadExpensesFromDatabase() {
        expenseList.clear();
        expenseList.addAll(expenseDatabase.expenseDao().getAll());
        refreshDashboard();
    }

    private void requestSmsPermissionsIfNeeded() {
        List<String> missingPermissions = new ArrayList<>();

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.READ_SMS);
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
                != PackageManager.PERMISSION_GRANTED) {
            missingPermissions.add(Manifest.permission.RECEIVE_SMS);
        }

        if (!missingPermissions.isEmpty()) {
            ActivityCompat.requestPermissions(
                    this,
                    missingPermissions.toArray(new String[0]),
                    SMS_PERMISSION_REQUEST_CODE
            );
        }
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            @NonNull String[] permissions,
            @NonNull int[] grantResults
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode == SMS_PERMISSION_REQUEST_CODE) {
            boolean granted = grantResults.length > 0;

            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    granted = false;
                    break;
                }
            }

            int message = granted
                    ? R.string.sms_permission_granted
                    : R.string.sms_permission_denied;

            Snackbar.make(binding.getRoot(), message, Snackbar.LENGTH_SHORT).show();
        }
    }

    private void refreshDashboard() {
        double total = 0;
        Map<String, Double> categoryMap = new LinkedHashMap<>();

        for (Expense expense : expenseList) {
            total += expense.getAmount();
            categoryMap.put(
                    expense.getCategory(),
                    categoryMap.getOrDefault(expense.getCategory(), 0.0) + expense.getAmount()
            );
        }

        binding.totalValue.setText(currencyFormat.format(total));
        binding.scoreValue.setText(String.valueOf(Math.round(AnalysisEngine.financialScore(expenseList))));
        binding.emotionalValue.setText(
                getString(R.string.percent_template, AnalysisEngine.emotionalPercent(expenseList))
        );
        binding.impulseValue.setText(String.valueOf(AnalysisEngine.impulseCount(expenseList)));
        binding.suggestionValue.setText(AnalysisEngine.generateSuggestion(expenseList));
        binding.microSpendingValue.setText(AnalysisEngine.detectMicroSpending(expenseList));

        List<CategoryTotal> categoryTotals = new ArrayList<>();
        categoryMap.entrySet().stream()
                .sorted(Map.Entry.comparingByValue(Comparator.reverseOrder()))
                .forEach(entry -> categoryTotals.add(new CategoryTotal(entry.getKey(), entry.getValue())));

        categorySummaryAdapter.submitList(categoryTotals, total);
        expenseAdapter.submitList(expenseList);

        boolean hasData = !expenseList.isEmpty();
        binding.emptyExpensesText.setVisibility(hasData ? View.GONE : View.VISIBLE);
        binding.emptyCategoryText.setVisibility(hasData ? View.GONE : View.VISIBLE);
    }

    private String valueOf(Object value) {
        return value == null ? "" : value.toString().trim();
    }
}
