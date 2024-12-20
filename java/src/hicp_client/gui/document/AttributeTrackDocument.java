package hicp_client.text;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;

import hicp.message.AttributeInfo;
import hicp.message.AttributeListInfo;
import hicp.message.AttributeRange;
import hicp.message.BooleanAttributeRange;
import hicp.message.command.ContentInfo;

public class AttributeTrackDocument
    extends javax.swing.text.PlainDocument
{
    private static final Logger LOGGER =
        Logger.getLogger( AttributeTrackDocument.class.getName() );

    public static abstract class Range {
        public int length = 0;

        public Range(final int newLength) {
            length = newLength;
        }

        public abstract boolean canMerge(Range otherRange);
        public abstract boolean isDefault();
        public abstract AttributeRange newAttributeRange();
    }

    // Range from message is immutable, we need mutable ranges.
    public static class ValueRange extends Range {
        public String value = null;

        public ValueRange(final int newLength) {
            this(newLength, null);
        }

        public ValueRange(final int newLength, final String newValue) {
            super(newLength);
            value = newValue;
        }

        public ValueRange(final AttributeRange range) {
            super(range.length);
            value = range.value;
        }

        @Override
        public boolean canMerge(Range otherRange) {
            if (!(otherRange instanceof ValueRange)) {
                return false;
            }
            final var valueRange = (ValueRange)otherRange;

            final boolean sameValue;
            if (null != value) {
                // value is not default, see if .equal to prev range value.
                sameValue = value.equals(valueRange.value);
            } else {
                // value is default, see if == to prev range value.
                sameValue = (value == valueRange.value);
            }
            return sameValue;
        }

        @Override
        public boolean isDefault() {
            return (null == value);
        }

        @Override
        public AttributeRange newAttributeRange() {
            return new AttributeRange(length, value);
        }
    }

    public static class BooleanRange extends Range {
        public boolean isOn = false;

        public BooleanRange(final int newLength) {
            this(newLength, false);
        }

        public BooleanRange(final int newLength, final boolean newIsOn) {
            super(newLength);
            isOn = newIsOn;
        }

        public BooleanRange(final AttributeRange range) {
            this(range.length);
        }

        public BooleanRange(final BooleanAttributeRange range) {
            super(range.length);
            isOn = range.isOn;
        }

        @Override
        public boolean canMerge(Range otherRange) {
            if (!(otherRange instanceof BooleanRange)) {
                return false;
            }
            final var booleanRange = (BooleanRange)otherRange;

            return (isOn == booleanRange.isOn);
        }

        @Override
        public boolean isDefault() {
            return (false == isOn);
        }

        @Override
        public AttributeRange newAttributeRange() {
            // on/off is implied by position - calling code must ensure order
            // is correct.
            return new AttributeRange(length);
        }
    }

    public interface RangeType<T extends Range> {
        public T newRange(final int length);
        public T newRange(final AttributeRange range);
    }

    public static class RangeTypes {
        public static final RangeType<ValueRange> VALUE =
            new RangeType<ValueRange>() {
                public ValueRange newRange(final int length) {
                    return new ValueRange(length);
                }
                public ValueRange newRange(final AttributeRange range) {
                    return new ValueRange(range);
                }
            };
        public static final RangeType<BooleanRange> BOOLEAN =
            new RangeType<BooleanRange>() {
                public BooleanRange newRange(final int length) {
                    return new BooleanRange(length);
                }
                public BooleanRange newRange(final AttributeRange range) {
                    return new BooleanRange(range);
                }
            };
    }

    // Just tracks HICP attributes, because JTextField does not actually
    // support any, but HICP wants them tracked and returned based on edits
    // anyway.

    // Value attribute ranges without values are the same value (default), so
    // they can be merged.
    // Boolean attribute ranges toggle, so cannot be merged, so they have to be
    // treated differently. Use different maps for each.
    protected Map<String, List<ValueRange>> attributeValuesMap = null;
    protected Map<String, List<BooleanRange>> attributeBooleanMap = null;

    public AttributeTrackDocument setAttributeListInfo(
        final AttributeListInfo newAttributeList
    ) {
        attributeValuesMap = new HashMap<>();
        attributeBooleanMap = new HashMap<>();

        if (null == newAttributeList) {
            return this;
        }
        for (final var attribute : newAttributeList) {
            // Convert attribute position + range list to all ranges to simplify
            // insert/remove.
            if (attribute.hasValues()) {
                setValueAttribute(attribute);
            } else {
                setBooleanAttribute(attribute);
            }
        }
        return this;
    }

    protected AttributeTrackDocument setValueAttribute(
        final AttributeInfo attribute
    ) {
        final List<ValueRange> rangeList = new ArrayList<>();

        // Need to insert a default range if position > 0.
        final int position = attribute.position;
        if (0 < position) {
            final var firstRange = new ValueRange(position);
            rangeList.add(firstRange);
        }
        for (final var range : attribute.getRangeList()) {
            final var newRange = new ValueRange(range);
            rangeList.add(newRange);
        }
        attributeValuesMap.put(attribute.name, rangeList);

        return this;
    }

    protected AttributeTrackDocument setBooleanAttribute(
        final AttributeInfo attribute
    ) {
        final List<BooleanRange> rangeList = new ArrayList<>();

        // First boolean range from attribute command is always on, so
        // need to insert an off range for boolean range
        // if position > 0.
        final int position = attribute.position;
        if (0 < position) {
            final var firstRange = new BooleanRange(position);
            rangeList.add(firstRange);
        }
        for (final var range : attribute.getBooleanRangeList()) {
            final var newRange = new BooleanRange(range);
            rangeList.add(newRange);
        }
        attributeBooleanMap.put(attribute.name, rangeList);

        return this;
    }

    public AttributeTrackDocument modifyAttributeListInfo(
        final AttributeListInfo newAttributeList
    ) {
        if (null == newAttributeList) {
            return this;
        }
        for (final var newAttribute : newAttributeList) {
            if (newAttribute.hasValues()) {
                final var rangeList =
                    attributeValuesMap.get(newAttribute.name);
                if (null == rangeList) {
                    // This is a new attribute.
                    setValueAttribute(newAttribute);
                } else {
                    changeRangeList(
                        newAttribute, RangeTypes.VALUE, rangeList
                    );
                }
            } else {
                final var rangeList =
                    attributeBooleanMap.get(newAttribute.name);
                if (null == rangeList) {
                    // This is a new attribute.
                    setBooleanAttribute(newAttribute);
                } else {
                    changeRangeList(
                        newAttribute, RangeTypes.BOOLEAN, rangeList
                    );
                }
            }
        }
        return this;
    }

    private <T extends Range> AttributeTrackDocument changeRangeList(
        final AttributeInfo newAttribute,
        final RangeType<T> rangeType,
        final List<T> rangeList
    ) {
        // Would it be simpler to just copy ranges to new list, rather than
        // modify the existing list?

        // How long is the span for the new list?
        final int position = newAttribute.position;
        int newRangeListLim = position;
        for (final var newAttributeRange : newAttribute.getRangeList()) {
            newRangeListLim += newAttributeRange.length;
        }
        // Replace existing attributes with new ones.
        int rangeIdx = 0;
        int rangeStart = 0;
        {
            Range range = null;
            int nextRangeStart = rangeStart;
            // Find first range affected by new range (start position).
            for (;;) {
                if (rangeIdx >= rangeList.size()) {
                    // No attribute range found, append new ranges.
                    break;
                }
                range = rangeList.get(rangeIdx);
                nextRangeStart = rangeStart + range.length;
                if (nextRangeStart >= position) {
                    // Found it.
                    break;
                }
                rangeIdx++;
                rangeStart = nextRangeStart;
            }
            // Truncate range if needed.
            if (nextRangeStart > position) {
                final int overlap = position - rangeStart;
                range.length -= overlap;
            }
        }
        rangeIdx++;
        // Remove ranges replaced by new ranges.
        {
            Range range = null;
            int nextRangeStart = rangeStart;
            for(;;) {
                if (rangeIdx >= rangeList.size()) {
                    // No attribute range found, append new ranges.
                    break;
                }
                range = rangeList.get(rangeIdx);
                nextRangeStart = rangeStart + range.length;
                if (nextRangeStart > newRangeListLim) {
                    // Last range goes past end, truncate instead of remove.
                    final int overlap =
                        newRangeListLim - rangeStart;
                    range.length -= overlap;
                    break;
                }
                rangeList.remove(rangeIdx);
                // Next range shifted to this index, don't move
                // index to next.
            }
            // If there is a gap between the last exiting range and
            // first new range, insert a default range.
            if (nextRangeStart < position) {
                final int newLength = position - nextRangeStart;
                final var newRange = rangeType.newRange(newLength);
                rangeList.add(rangeIdx, newRange);
                rangeIdx++;
            }
        }
        // Insert new ranges.
        for (final var newAttributeRange : newAttribute.getRangeList()) {
            final var newRange = rangeType.newRange(newAttributeRange);
            rangeList.add(rangeIdx, newRange);
            rangeIdx++;
        }
        return this;
    }

    public AttributeListInfo getAttributeListInfo() {
        final var newAttributeListInfo = new AttributeListInfo();

        // Add value attributes.
        for (final var attributeName : attributeValuesMap.keySet()) {
            final var rangeList = attributeValuesMap.get(attributeName);

            addAttributeInfo(attributeName, rangeList, newAttributeListInfo);
        }

        // Add boolean attributes.
        for (final var attributeName : attributeBooleanMap.keySet()) {
            final var rangeList = attributeBooleanMap.get(attributeName);

            addAttributeInfo(attributeName, rangeList, newAttributeListInfo);
        }

        return newAttributeListInfo;
    }

    protected AttributeTrackDocument addAttributeInfo(
        final String attributeName,
        final List<? extends Range> rangeList,
        final AttributeListInfo attributeListInfo
    ) {
        // If the first range is a default value, then use that as the
        // attribute start position.
        final int position;
        final List<AttributeRange> newRangeList;
        if (0 < rangeList.size()) {
            final int rangesSkipped;
            {
                final var firstRange = rangeList.get(0);
                if (firstRange.isDefault()) {
                    position = firstRange.length;
                    rangesSkipped = 1;
                } else {
                    position = 0;
                    rangesSkipped = 0;
                }
            }
            newRangeList =
                rangeList
                    .stream()
                    .skip(rangesSkipped)  // If first was used for position.
                    .map(r -> r.newAttributeRange())
                    .collect(Collectors.toList());
        } else {
            position = 0;
            newRangeList = new ArrayList<>();
        }
        final var attribute =
            new AttributeInfo(attributeName, position, newRangeList);

        attributeListInfo.addAttributeInfo(attribute);
        return this;
    }

    protected AttributeTrackDocument mergeValues() {
        for (final var attributeName : attributeValuesMap.keySet()) {
            final var rangeList = attributeValuesMap.get(attributeName);

            mergeRange(rangeList);
        }
        return this;
    }

    protected AttributeTrackDocument mergeBoolean() {
        for (final var attributeName : attributeBooleanMap.keySet()) {
            final var rangeList = attributeBooleanMap.get(attributeName);

            mergeRange(rangeList);
        }
        return this;
    }

    protected AttributeTrackDocument mergeRange(
        final List<? extends Range> rangeList
    ) {
        int rangeLim = rangeList.size();
        for (int rangeIdx = 1; rangeIdx < rangeLim; /* ... */) {
            final var range = rangeList.get(rangeIdx);

            final int prevRangeIdx = rangeIdx - 1;
            final  var prevRange = rangeList.get(prevRangeIdx);

            // If current and previous have same value, add current length
            // to previous length and remove current.
            final boolean canMerge = range.canMerge(prevRange);;
            if (canMerge) {
                prevRange.length += range.length;
                rangeList.remove(rangeIdx);
                rangeLim--;

                // Next range has shifted to this position, don't increment
                // rangeIdx.
            } else if (0 == range.length) {
                // Range with no length is not useful.
                rangeList.remove(rangeIdx);
                rangeLim--;

                // Next range has shifted to this position, don't increment
                // rangeIdx.
            } else {
                rangeIdx++;
            }
        }
        return this;
    }

    @Override
    public void insertString(int offset, String str, AttributeSet a)
        throws BadLocationException
    {
        insertForAttributes(offset, str);
        super.insertString(offset, str, a);
    }

    public AttributeTrackDocument insertForAttributes(
        final int offset,
        final String str
    ) {
        final int len = str.length();
        if (null != attributeValuesMap) {
            for (final var rangeList : attributeValuesMap.values()) {
                insertForRangeList(offset, len, rangeList);
            }
            mergeValues();
        }
        if (null != attributeBooleanMap) {
            for (final var rangeList : attributeBooleanMap.values()) {
                insertForRangeList(offset, len, rangeList);
            }
            mergeBoolean();
        }
        return this;
    }

    protected AttributeTrackDocument insertForRangeList(
        final int offset,
        final int len,
        final List<? extends Range> rangeList
    ) {
        // Increase the length of whatever range this is inserted to.
        int rangeStart = 0;
        for (final var range : rangeList) {
            final int nextRangeStart = rangeStart + range.length;
            /*
                Typically inserting after the end of a range extends it,
                rather than extending the next range. E.g for underline
                (first position is 0):
                  "abc"
                    -
                Insert "b" at position 2:
                  "abbc"
                    --
                But insert "b" at position 1:
                  "abbc"
                     -
                Exception for range starting at 0 of course (no previous range).
             */
            final boolean isOffsetAfterStart =
                rangeStart == 0
                    ? (rangeStart <= offset)
                    : (rangeStart < offset);
            final boolean isOffsetBeforeEnd =
                (nextRangeStart >= offset);

            if (isOffsetAfterStart && isOffsetBeforeEnd) {
                range.length += len;

                // Doesn't affect any other attribute range.
                return this;
            }
            rangeStart = nextRangeStart;
        }
        return this;
    }

    @Override
    public void remove(int offset, int len)
        throws BadLocationException
    {
        removeForAttributes(offset, len);
        super.remove(offset, len);
    }

    public AttributeTrackDocument removeForAttributes(
        final ContentInfo.DeleteInfo deleteInfo
    ) {
        return removeForAttributes(deleteInfo.position, deleteInfo.length);
    }

    public AttributeTrackDocument removeForAttributes(
        final int offset,
        final int len
    ) {
        if (null != attributeValuesMap) {
            for (final var rangeList : attributeValuesMap.values()) {
                removeForRangeList(offset, len, rangeList);
            }
            mergeValues();
        }
        if (null != attributeBooleanMap) {
            for (final var rangeList : attributeBooleanMap.values()) {
                removeForRangeList(offset, len, rangeList);
            }
            mergeBoolean();
        }
        return this;
    }

    protected AttributeTrackDocument removeForRangeList(
        final int removeStart,
        final int len,
        final List<? extends Range> rangeList
    ) {
        final int removeLim = removeStart + len;

        // Find all ranges covered by this removal. Shorten ranges which
        // just overlap, delete ranges which are within the removal.
        int deletedLen = 0;
        int rangeStart = 0;
        int rangeLim = rangeList.size();
rangeLoop:
        for (int rangeIdx = 0; rangeIdx < rangeLim; /* ... */ ) {
            final var range = rangeList.get(rangeIdx);
            final int nextRangeStart = rangeStart + range.length;
            /*
                Range to remove: |   |
                Attribute range: +---+

                    +---+ |   |   Do nothing
                        +---+ |   Reduce length by overlap
                        +-----+   Reduce length by overlap (= len)
                        +-------+ Reduce length by len
                          +--+|   Remove range
                          |+-+|   Remove range
                          +---+   Remove range
                          |+--+   Remove range
                          | +---+ Reduce length by overlap and quit.
                          +-----+ Reduce length by overlap (= len) and quit.
             */
            {
                final boolean willRemoveRangeStart =
                    (rangeStart >= removeStart);

                final boolean willRemoveRangeEnd =
                    (nextRangeStart <= removeLim);

                if (nextRangeStart <= removeStart) {
                    // No overlap, skip.
                    // This is necessary to simplify the other range checks.
                } 
                else if (!willRemoveRangeStart && willRemoveRangeEnd)
                {
                    // Remove from right of range.
                    final int overlap = nextRangeStart - removeStart;
                    range.length -= overlap;
                } 
                else if (!willRemoveRangeStart && !willRemoveRangeEnd)
                {
                    // Remove from middle fo range.
                    range.length -= len;
                } 
                else if (willRemoveRangeStart && willRemoveRangeEnd)
                {
                    // All of range removed.
                    range.length = 0;
                }
                else if (willRemoveRangeStart && !willRemoveRangeEnd)
                {
                    // Remove from left of range.
                    final int overlap = removeLim - rangeStart;
                    range.length -= overlap;
                }
            }
            // Do actual delete,
            // or clean up after one or more ranges deleted.
            final boolean shouldDelete = (0 == range.length);

            if (shouldDelete) {
                // Do actual delete now.
                rangeList.remove(rangeIdx);
                rangeLim--;

                deletedLen++;

                // Next range has shifted to this position, don't increment
                // rangeIdx.
            } else {
                rangeIdx++;
            }

            if (nextRangeStart >= removeLim) {
                // Next range is past end of remove range, no more ranges
                // to check.
                break rangeLoop;
            }
            rangeStart = nextRangeStart;
        }
        return this;
    }
}
