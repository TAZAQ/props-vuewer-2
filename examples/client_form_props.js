import { PropsTypes } from '@/vue_present/_base/PropsTypes'

export default {
  strict: Boolean,
  disabled: Boolean,
  targetedDisable: {
    type: Object,
    default: () => ({}),
  },
  errors: {
    type: Object,
    required: true,
  },
  name: {
    type: String,
    required: true,
  },
  surname: {
    type: String,
    required: true,
  },
  secondName: {
    type: String,
    required: true,
  },
  birthdate: {
    type: String,
    default: null,
  },
  phone: {
    type: String,
    required: true,
  },
  sex: {
    type: Number,
    default: 0,
  },
  entryOnlineAccess: {
    type: String,
    required: true,
  },
  /**
   * @type {import('vue').PropOptions<{id: number, title: string}[]>}
   */
  onlineAccessOptions: {
    type: Array,
    required: true,
  },
  /**
   * @type {import('vue').PropOptions<{id: number, title: string}[]>}
   */
  sexes: {
    type: Array,
    required: true,
  },
  /**
   * @type {import('vue').PropOptions<number[]>}
   */
  groups: {
    type: Array,
    required: true,
  },
  personalDiscount: {
    type: Number,
    required: true,
  },
  email: {
    type: String,
    default: null,
  },
  additional: {
    type: String,
    default: null,
  },
  /**
   * @type {import('vue').PropOptions<{id: number, title: string}[]>}
   */
  clientGroups: {
    type: Array,
    required: true,
  },
  clinicId: {
    type: Number,
    default: null,
  },

  snils: {
    type: String,
    default: null,
  },

  patientCardNumber: PropsTypes.String(''),
}
