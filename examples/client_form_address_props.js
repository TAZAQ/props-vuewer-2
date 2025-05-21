export default {
  activeTab: {
    type: String,
    default: 'TAB_ADDRESS',
  },
  errors: {
    type: Object,
    required: true,
  },
  fullAddress: {
    type: String,
    default: null,
  },
  index: {
    type: String,
    default: null,
  },
  country: {
    type: String,
    default: null,
    validator: (prop) => typeof prop === 'string' || prop === null,
  },
  region: {
    type: String,
    default: null,
    validator: (prop) => typeof prop === 'string' || prop === null,
  },
  nsiRussianSubjectId: {
    type: Number,
    default: null,
  },
  area: {
    type: String,
    default: null,
    validator: (prop) => typeof prop === 'string' || prop === null,
  },
  city: {
    type: String,
    default: null,
    validator: (prop) => typeof prop === 'string' || prop === null,
  },
  street: {
    type: String,
    default: null,
  },
  house: {
    type: String,
    default: null,
  },
  flat: {
    type: String,
    default: null,
  },
  targetedDisable: {
    type: Object,
    default () {
      return {}
    },
  },
}
