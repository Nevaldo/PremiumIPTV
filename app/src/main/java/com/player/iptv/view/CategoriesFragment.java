package com.player.iptv.view;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.player.iptv.R;
import com.player.iptv.adapter.CategoryAdapter;
import com.player.iptv.model.Category;

import java.util.ArrayList;
import java.util.List;

public class CategoriesFragment extends Fragment {

    private RecyclerView recyclerCategories;
    private CategoryAdapter adapter;
    private List<Category> categoryList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_categories, container, false);

        recyclerCategories = view.findViewById(R.id.recyclerCategories);

        recyclerCategories.setLayoutManager(new GridLayoutManager(getContext(), 4));

        categoryList = new ArrayList<>();

        Category acao = new Category("1","Ação",1);
        acao.setImageResId(R.drawable.ic_cat_acao);
        categoryList.add(acao);

        Category drama = new Category("2","Drama", 2);
        drama.setImageResId(R.drawable.ic_cat_drama);
        categoryList.add(drama);

        Category comedia = new Category("3","Comédia", 3);
        comedia.setImageResId(R.drawable.ic_cat_comedia);
        categoryList.add(comedia);

        Category terror = new Category("4","Terror", 4);
        terror.setImageResId(R.drawable.ic_cat_terror);
        categoryList.add(terror);

        Category esportes = new Category("5","Esportes", 5);
        esportes.setImageResId(R.drawable.ic_cat_esportes);
        categoryList.add(esportes);

        Category animacao = new Category("6","Animação", 6);
        animacao.setImageResId(R.drawable.ic_cat_animacao);
        categoryList.add(animacao);

        adapter = new CategoryAdapter();
        adapter.setViewType(CategoryAdapter.VIEW_TYPE_CARD);
        adapter.submitList(categoryList);

        recyclerCategories.setAdapter(adapter);

        return view;
    }

}