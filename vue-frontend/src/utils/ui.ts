import { createVNode } from 'vue'
import { message, Modal } from 'ant-design-vue'
import {
  ExclamationCircleOutlined,
  CloseCircleOutlined,
  InfoCircleOutlined,
  CheckCircleOutlined,
} from '@ant-design/icons-vue'

type ConfirmType = 'warning' | 'error' | 'info' | 'success'

interface ConfirmOptions {
  title?: string
  content: string
  okText?: string
  cancelText?: string
  type?: ConfirmType
}

const confirmIcons: Record<ConfirmType, typeof ExclamationCircleOutlined> = {
  warning: ExclamationCircleOutlined,
  error: CloseCircleOutlined,
  info: InfoCircleOutlined,
  success: CheckCircleOutlined,
}

function getConfirmIcon(type: ConfirmType) {
  return createVNode(confirmIcons[type])
}

function confirm(options: ConfirmOptions): Promise<void> {
  return new Promise((resolve, reject) => {
    Modal.confirm({
      title: options.title,
      content: options.content,
      okText: options.okText ?? '确定',
      cancelText: options.cancelText ?? '取消',
      icon: getConfirmIcon(options.type ?? 'warning'),
      centered: true,
      onOk: () => resolve(),
      onCancel: () => reject('cancel'),
    })
  })
}

export const ui = {
  success(content: string) {
    message.success(content)
  },
  error(content: string) {
    message.error(content)
  },
  warning(content: string) {
    message.warning(content)
  },
  info(content: string) {
    message.info(content)
  },
  confirm,
}
