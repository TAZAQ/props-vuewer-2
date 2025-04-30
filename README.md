# Plugin for intellij idea - "PropsVuewer"

## Description
The plugin show (inline) the props in vue 2 components added to the props using the spread operator or during assignment of imported props.

## Current status
it works, but crookedly
![](https://github.com/user-attachments/assets/1432696f-2a9f-480b-a86f-f6b18d6badfd)


## Example of MLazyListBaseProps
```typescript
export const MLazyListBaseProps = {
  value: { type: [Array, Object], default: null, required: true },
  label: { type: String, default: '' },
  optionLabel: { type: String, default: 'title' },
  filterable: { type: Boolean, default: true },
  multiple: Boolean,
  fullWidth: Boolean,
}
```

## Example Case 1 - spread operator
### Example Input
```vue
  props: {
    ...MLazyListBaseProps, // imported from other file
    list: { type: Object as PropType<MLazyListServiceApi<unknown, unknown>>, required: true },
  },
```

### Expected output by Example
```vue
  props: {
    ...MLazyListBaseProps,
        // value: [Array, Object], default: null, required: true
        // label: String, default: ''
        // optionLabel: String, default: 'title'
        // filterable: Boolean, default: true
        // multiple: Boolean,
        // fullWidth: Boolean,
    list: { type: Object as PropType<MLazyListServiceApi<unknown, unknown>>, required: true },
  },
```

## Example Case 2 - assignment
### Example Input
```vue
  props: MLazyListBaseProps // imported from other file
```

### Expected output by Example
```vue
  props: MLazyListBaseProps,
    // value: [Array, Object], default: null, required: true
    // label: String, default: ''
    // optionLabel: String, default: 'title'
    // filterable: Boolean, default: true
    // multiple: Boolean,
    // fullWidth: Boolean,
```

