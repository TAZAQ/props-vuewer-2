export default {
  activeTab: {
    type: String,
    default: 'TAB_ADDRESS',
  },
  /**
   * @type {import('vue').PropOptions<ClientTabsErrors>}
   */
  errors: {
    type: Object,
    default: null,
  },

  documentType: {
    type: Number,
    default: null,
  },
  documentSeries: {
    type: String,
    default: null,
  },
  documentNumber: {
    type: String,
    default: null,
  },
  whoIssue: {
    type: String,
    default: null,
  },
  issueDate: {
    type: String,
    default: null,
    validator: (prop) => typeof prop === 'string' || prop === null,
  },
  position: {
    type: String,
    default: null,
  },
  nsiProfession: {
    type: Object,
    default: null,
  },
  department: {
    type: String,
    default: null,
  },
  oms: {
    type: String,
    default: null,
  },
  inn: {
    type: String,
    default: null,
  },
  snils: {
    type: String,
    default: null,
  },
  /**
   * @type {import('vue').PropOptions<{id: number, title: string}>}
   */
  documentTypes: {
    type: Array,
    required: true,
  },
  /**
   * @type {import('vue').PropOptions<{id: number, title: string}>}
   */
  company: {
    type: Object,
    default: () => null,
  },
  disabled: {
    type: Boolean,
  },
  targetedDisable: {
    type: Object,
    default () {
      return {}
    },
  },
}
