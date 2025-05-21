
export const namedProps = {
    bProp: Boolean,
    nProp: { type: Number, default: 228 },
    sProp: { type: String, required: true },
    aProp: { type: Array, required: true, validator: (value) => !!value.length },
    oProp: { type: Object, required: true, default: () => ({}), validator: (value) => !!Object.keys(value).length }
}