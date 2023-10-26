package hicp_client.text;

public interface TextItemAdapterListener {
    /**
        Implementer should save text item adapter tla,
        then call tla.setAdapter(this).
     */
    public void setAdapter(TextItemAdapter tia);

    /**
        Implementer apply text to the displayed component.
     */
    public void setText(String text);

    /**
        Implementer call removeAdapter() on the saved text item adapter.
     */
    public void removeAdapter();
}
