export default {
  disabled: {
    type: Boolean,
  },

  clinicId: {
    type: Number,
    default: null,
  },
  /**
   * @type {import('vue').PropOptions<Object>}
   *
   * attractionSourceId: required
   */
  validationMessages: {
    type: Object,
    required: true,
  },

  serviceSearchQuery: {
    type: String,
    default: '',
  },

  servicesSearchResultArray: {
    type: Array,
    default: () => [],
  },

  serviceSearchAllowSearch: {
    type: Boolean,
    default: false,
  },

  serviceSearchLoading: {
    type: Boolean,
    default: false,
  },

  serviceNotResults: {
    type: Boolean,
    default: false,
  },

  /**
   * @type {import('vue').PropOptions<{id: Number, title: String, price: Number}[]>}
   */
  entryTypes: {
    type: Array,
    default: () => [],
  },

  currencySymbol: {
    type: String,
    required: true,
  },

  currencyFormat: {
    type: Object,
    required: true,
  },

  note: {
    type: String,
    default: '',
  },

  clientComment: {
    type: String,
    default: '',
  },

  /**
   * @type {import('vue').PropOptions<Number[]>}
   */
  durationsArray: {
    type: Array,
    default: () => [],
    required: true,
  },

  duration: {
    type: Number,
    default: MIN_DURATION,
    required: true,
  },

  minDuration: {
    type: Number,
    default: MIN_DURATION,
    required: false,
  },

  maxDuration: {
    type: Number,
    required: true,
  },

  /**
   * @type {import('vue').PropOptions<{id: Number, title: String}[]>}
   */
  appointmentTypesArray: {
    type: Array,
    default: () => [],
    required: true,
  },

  appointmentTypeId: {
    type: Number,
    default: null,
    validator: (prop) => typeof prop === 'number' || prop === null,
  },

  appointmentTypeIdDefault: {
    type: Number,
    default: 1,
  },

  referral: {
    type: Object,
    default: () => null,
  },

  attractionSourceId: {
    type: Number,
    default: null,
  },

  /**
   * @type {import('vue').PropOptions<{id: Number, title: String, default: boolean}[]>}
   */
  attractionSourceArray: {
    type: Array,
    default: () => [],
  },

  byDms: {
    type: Boolean,
    default: false,
  },

  appointmentStatus: {
    type: String,
    default: 'ПОДТВЕРДИ!',
    required: true,
  },

  /**
   * @type {import('vue').PropOptions<{id: String, title: String}[]>}
   */
  appointmentSource: {
    type: Object,
    default: () => ({}),
    required: true,
  },

  addToWaitingList: {
    type: Boolean,
    default: false,
  },

  createdBy: {
    type: String,
    default: '',
  },

  updatedBy: {
    type: String,
    default: '',
  },
}
